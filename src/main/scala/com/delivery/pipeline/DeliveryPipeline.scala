package com.delivery.pipeline

import akka.Done
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffsetBatch}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//Streams from order and courier topics
object DeliveryPipeline {
  type KeyType = String
  type ValueType = Array[Byte] //This may change to Array[Byte] or ResourceEvent
  type CommittableMessageType = CommittableMessage[KeyType, ValueType]
  type ProcessCallback = ConsumerRecord[KeyType, ValueType] => Future[Unit]
  case class ConsumingMessage(msg: CommittableMessageType) { def isTombstone: Boolean = msg.record.value == null }

  val consumeParallelism = 100

  private val commitBatchSize: Int = 1000
  private val commitBatchTimeout: FiniteDuration = 10.second

  def build(topics: List[String], consumerSettings: ConsumerSettings[String, Array[Byte]], processCb: ProcessCallback)
           (implicit ec: ExecutionContext): RunnableGraph[Future[Done]] = {

    val consumeSource = Consumer.committableSource(consumerSettings, Subscriptions.topics(topics: _*))
      .map(m => ConsumingMessage(m))

    // Handle messages from consume source
    val consumeFlow = Flow[ConsumingMessage].mapAsync(consumeParallelism) {
      case msg: ConsumingMessage if !msg.isTombstone => processCb(msg.msg.record).map(_ => msg)
      case msg => Future.successful(msg)
    }

    // Commit offsets after messages handled by consume flow
    val commitFlow = Flow[ConsumingMessage].collect[CommittableMessageType] { case msg: ConsumingMessage => msg.msg }
      .groupedWithin(commitBatchSize, commitBatchTimeout)
      .map(group => group.foldLeft(CommittableOffsetBatch.empty)((batch, elem) => batch.updated(elem.committableOffset)))
      .mapAsync(1)(batch => batch.commitScaladsl().andThen {
        case Success(_) => // NOOP
        case Failure(t) => //println(s"Commit failed for offsets ${batch.offsets()} due to $t . StackTrace : ${t.getStackTrace}")
      })

    consumeSource
      .via(consumeFlow)
      .via(commitFlow)
      //.via(killSwitch.flow)//TODO - check if needed
      .toMat(Sink.ignore)(Keep.right)

  }
}