package com.delivery

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.util.UUID

import akka.actor.Actor
import akka.kafka.ConsumerSettings
import akka.stream.ActorMaterializer
import com.delivery.DeliveryManager.DeliveryState
import com.delivery.model._
import com.delivery.pipeline.DeliveryPipeline
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.Future

object DeliveryManager{
  case class DeliveryState(orders: Map[UUID, Order],
                           couriers: Map[UUID, Courier],
                           assignments: Set[Assignment])
}

class DeliveryManager extends Actor{

  override def preStart = {
    super.preStart
    //val timer = context.system.scheduler.schedule(1 second, 1 second, context.self, UpdateReader)(context.dispatcher)
    context.become(live(DeliveryState(Map.empty, Map.empty, Set.empty)))
  }

  implicit val ec = context.dispatcher
  implicit val mat = ActorMaterializer()

  val topics = List("delivery.order", "delivery.courier")

  def receive = Actor.emptyBehavior

  def live(ds: DeliveryState): Receive = {
    case Build =>
      val processEventCb: ConsumerRecord[String, Array[Byte]] => Future[Unit] = event => {
        val byteIn = new ByteArrayInputStream(event.value())
        val objIn = new ObjectInputStream(byteIn).readObject()
        println(s"Received ${objIn}")

        objIn match {
          case cr: Courier => //Add Courier
            self ! Add_Courier(cr)

          case or: Order => //Add Order (and assign?)
            self ! Add_Order(or)
        }

        Future.successful()
      }

      val bootstrapServers = "localhost:9092"//todo - make configurable

      val config = context.system.settings.config.getConfig("akka.kafka.consumer")
      val consumerSettings =
        ConsumerSettings(config, new StringDeserializer, new ByteArrayDeserializer)
          .withBootstrapServers(bootstrapServers)
          .withGroupId("group1")
          .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

      DeliveryPipeline.build(topics, consumerSettings, processEventCb).run()

    case Add_Courier(cr: Courier) =>
      val upd = ds.couriers.updated(cr.courier_id, cr)
      context.become(live(ds.copy(couriers = upd)))

    case Add_Order(or: Order) =>
      val upd = ds.orders.updated(or.order_id, or)
      self ! Assign_Courier(or)
      context.become(live(ds.copy(orders = upd)))

    case Mark_Courier(id: UUID, available: Boolean) =>
      val updated = ds.couriers.get(id) match {
        case Some(cr) => Courier(cr.courier_id, cr.name, cr.zone, available)
        case _ => throw new Exception(s"Courier with id $id not found")
      }
      context.become(live(ds.copy(couriers = ds.couriers.updated(id, updated))))

    case Get_Courier(orderId: UUID) =>
      ds.assignments.find(_.order_id == orderId) match {
        case Some(assgn) => context.sender() ! ds.couriers.get(assgn.courier_id)
        case _ =>
          {
            if(ds.orders.get(orderId).isEmpty) throw new Exception(s"Invalid order id ${orderId}")
            context.sender ! s"Courier not yet assigned for order $orderId"
          }

      }

    case Get_Orders(courierId: UUID) =>
      sender ! ds.assignments.filter(_.courier_id == courierId).map(assgn => ds.orders(assgn.order_id))

    case Assign_Courier(or: Order) =>
      val assgns = ds.couriers.values.find(cr => cr.zone == or.zone && cr.is_available) match {
        case Some(cr) => ds.assignments + Assignment(cr.courier_id, or.order_id)
        case _ => {
          sender ! s"Courier not Available for  ${or.order_id}"
          //TODO - retry?
          ds.assignments
        }
      }
      context.become(live(ds.copy(assignments = assgns)))

    case Get_All =>
      context.sender() ! ds

    case x =>
      println(s"Unknown event $x")
  }
}
