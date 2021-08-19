package com.delivery

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.util.{Properties, UUID}

import com.delivery.model._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.Random
object DeliveryProducerApp extends App {

  val props:Properties = new Properties()
  props.put("bootstrap.servers","localhost:9092")
  props.put("key.serializer",
    "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer",
    "org.apache.kafka.common.serialization.ByteArraySerializer")
  props.put("acks","all")
  val producer = new KafkaProducer[String, Array[Byte]](props)
  val topic = "delivery.order"
  try {
    for (i <- 0 to 15) {
      val zone = Zones(Random.nextInt(Zones.maxId))
      val order = Order(UUID.randomUUID(),"Some detail", zone, System.currentTimeMillis())
      val courier = Courier(UUID.randomUUID(),"Courier"+i, zone, true)


      val courierRecord = new ProducerRecord[String, Array[Byte]](topic, i.toString, serialize(courier))
      val courierMetadata = producer.send(courierRecord)
      printf(s"sent record(key=%s value=%s) " +
        "meta(partition=%d, offset=%d)\n",
        courierRecord.key(), courierRecord.value(),
        courierMetadata.get().partition(),
        courierMetadata.get().offset())

      val record = new ProducerRecord[String, Array[Byte]](topic, i.toString, serialize(order))
      val metadata = producer.send(record)
      printf(s"sent record(key=%s value=%s) " +
        "meta(partition=%d, offset=%d)\n",
        record.key(), record.value(),
        metadata.get().partition(),
        metadata.get().offset())
    }
  }catch{
    case e:Exception => e.printStackTrace()
  }finally {
    producer.close()
  }

  def serialize(data:Any): Array[Byte] = {
    try {
      val byteOut = new ByteArrayOutputStream()
      val objOut = new ObjectOutputStream(byteOut)
      objOut.writeObject(data)
      objOut.close()
      byteOut.close()
      byteOut.toByteArray
    }
    catch {
      case ex:Exception => throw new Exception(ex.getMessage)
    }
  }
}
