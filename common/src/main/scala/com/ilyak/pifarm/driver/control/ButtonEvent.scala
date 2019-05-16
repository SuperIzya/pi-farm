package com.ilyak.pifarm.driver.control

import cats.Eq
import com.ilyak.pifarm.{ Decoder, Measurement, Units }

case class ButtonEvent(value: Boolean) extends Measurement[Boolean]

object ButtonEvent {
  implicit val eq: Eq[ButtonEvent] = _.value == _.value

  implicit val unit: Units[ButtonEvent] = "control event: The Button is pressed"
  implicit val dec: Decoder[ButtonEvent] = msg => msg.split("\n").collect{
    case s if s.startsWith("the-button:") => ButtonEvent(s.contains("1"))
  }
}
