package com.delivery.model

import java.util.UUID
import com.delivery.model.Zones.Zone

case class Courier(courier_id: UUID, name: String, zone: Zone, is_available: Boolean)

case class Order(order_id: UUID, details: String, zone: Zone, added_at: Long)

case class Assignment(courier_id: UUID, order_id: UUID)

object Zones extends Enumeration {
  type Zone = Value

  val N, E, S, W = Value
}

//delivery manager messages
case object Build
case class Add_Courier(cr: Courier)
case class Add_Order(or: Order)
case class Mark_Courier(id: UUID, available: Boolean)
case class Get_Courier(orderId: UUID)
case class Get_Orders(courierId: UUID)
case class Assign_Courier(or: Order)
case object Get_All
