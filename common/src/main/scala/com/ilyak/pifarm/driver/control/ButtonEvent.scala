package com.ilyak.pifarm.driver.control

import cats.Eq
import com.ilyak.pifarm.{ Decoder, Units }

case class ButtonEvent(isOn: Boolean)

object ButtonEvent {
  implicit val eq: Eq[ButtonEvent] = _.isOn == _.isOn

  implicit val unit: Units[ButtonEvent] = "control event: is button pressed"
  implicit val dec: Decoder[ButtonEvent] = msg => Seq(ButtonEvent(msg.contains("1")))
}
