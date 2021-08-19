package com.delivery.test

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.delivery.DeliveryManager
import com.delivery.DeliveryManager.DeliveryState
import com.delivery.model.{Add_Courier, Add_Order, Assign_Courier, Courier, Get_All, Get_Courier, Get_Orders, Mark_Courier, Order, Zones}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class OrderMgmtSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll{

  "OrderMgmt" should {
    val mgrApi = system.actorOf(Props[DeliveryManager])
    "Add Couriers" in {

      val size = 15
      for (i <- 0 to size) {
        val zone = Zones(Random.nextInt(Zones.maxId))
        val courier = Courier(UUID.randomUUID(), "Courier" + i, zone, true)

        mgrApi ! Add_Courier(courier)
      }

      mgrApi ! Get_All
      val ds: DeliveryState = expectMsgType[DeliveryState]
      ds.couriers.size should be(size+1)
    }

    "Add orders" in {

      val size = 15
      for (i <- 0 to size) {
        val zone = Zones(Random.nextInt(Zones.maxId))
        val order = Order(UUID.randomUUID(), "Some detail", zone, System.currentTimeMillis())

        mgrApi ! Add_Order(order)
      }

      mgrApi ! Get_All
      val ds: DeliveryState = expectMsgType[DeliveryState]
      ds.orders.size should be(size+1)
    }

    "Mark Couriers Unavailable" in {

      mgrApi ! Get_All
      val ds: DeliveryState = expectMsgType[DeliveryState]
      for(cr <- ds.couriers)
        mgrApi ! Mark_Courier(cr._1, false)

      val order = ds.orders.values.head
      mgrApi ! Assign_Courier(order)
      expectMsg(s"Courier not Available for  ${order.order_id}")
    }
  }

  it should {
    val mgrApi = system.actorOf(Props[DeliveryManager])
    "Assign Couriers" in {
      //create 1 courier
      val cr = Courier(UUID.randomUUID(), "Courier", Zones.S, true)
      mgrApi ! Add_Courier(cr)

      //create 4 orders
      val or1 = Order(UUID.randomUUID(), "Urgent", Zones.S, System.currentTimeMillis())
      val or2 = Order(UUID.randomUUID(), "Fragile", Zones.S, System.currentTimeMillis())
      val or3 = Order(UUID.randomUUID(), "Shipment", Zones.S, System.currentTimeMillis())
      val or4 = Order(UUID.randomUUID(), "Fedex", Zones.S, System.currentTimeMillis())
      mgrApi ! Add_Order(or1)
      mgrApi ! Add_Order(or2)
      mgrApi ! Add_Order(or3)
      mgrApi ! Add_Order(or4)

      Thread.sleep(1000)

      //all 4 orders should get assigned to same courier
      mgrApi ! Get_Courier(or1.order_id)
      expectMsg(Some(cr))

      mgrApi ! Get_Orders(cr.courier_id)
      expectMsg(Set(or1, or2, or3, or4))
    }
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
