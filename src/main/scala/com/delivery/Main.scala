package com.delivery

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import com.delivery.api.DeliveryAPI
import com.delivery.model.Build

object Main extends App{

  implicit val system = ActorSystem("DeliveryMain")
  val manager = system.actorOf(Props[DeliveryManager])
  manager ! Build

  val api = new DeliveryAPI(manager)
  Http().newServerAt("localhost", 8081).bind(api.routes())

}
