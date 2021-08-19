package com.delivery.api

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import com.delivery.DeliveryManager.DeliveryState
import com.delivery.model._
import de.heikoseeberger.akkahttpjackson.JacksonSupport

import scala.concurrent.duration._
import scala.util.Success

class DeliveryAPI(mgrRef: ActorRef) extends JacksonSupport {
  import akka.http.scaladsl.server.Directives._

  case class MarkCourier(id: String, avlbl: Boolean)

  implicit val timeoutSec = 60.seconds
  implicit val serialization = org.json4s.jackson.Serialization
  def routes(): Route = {

    implicit val system = ActorSystem("DeliveryAPI")
    implicit val ec = system.dispatcher

    pathPrefix("api"){
      concat(
        //Retrieves the internal state
        path("status") {
          get {
            val f = mgrRef.ask(Get_All)(timeoutSec)
            onComplete(f) {
              case Success(ds: DeliveryState) => complete(ds)
              case _ => complete(StatusCodes.InternalServerError)
            }
          }
        },
        //Mark a courier's availability
        path("courier" / Segment) { id =>
          post {
            entity(as[String]){ mark => mgrRef ! Mark_Courier(UUID.fromString(id), mark!=null && mark.equalsIgnoreCase("true"))
              complete(StatusCodes.OK)
            }
          }
        },
        path("courier" ) {
          get {
            //Get courier for an order
            parameter("order") { (or) =>
              val f = mgrRef.ask(Get_Courier(UUID.fromString(or)))(timeoutSec)
              onComplete(f) {
                case Success(Some(cr: Courier)) => complete(cr)
                case Success(str: String) => complete(str)
                case _ => complete(StatusCodes.InternalServerError)
              }
            }
          }
        },
        path("orders") {
          get {
            //Get all orders this courier has to deliver
            parameter("courier") { (courier) =>
              val f = mgrRef.ask(Get_Orders(UUID.fromString(courier)))(timeoutSec)
              onComplete(f) {
                case Success(orders: Set[Order]) => complete(orders)
                case Success(str: String) => complete(str)
                case _ => complete(StatusCodes.InternalServerError)
              }
            }
          }
        }
      )
    }
  }

}