package com.ilyak.pifarm.io.device.arduino

import cats.Eq
import com.ilyak.pifarm.{ Decoder, Units }

case class Event(isOn: Boolean)

object Event {
  implicit val eq: Eq[Event] = _.isOn == _.isOn

  implicit val unit: Units[Event] = "default event"
  implicit val dec: Decoder[Event] = msg => Seq(Event(msg.contains("1")))
}
