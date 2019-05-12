package com.ilyak.pifarm.driver.control

import cats.Eq
import com.ilyak.pifarm.{ Decoder, Units }

case class ButtonEvent(isOn: Boolean)

object ButtonEvent {
  implicit val eq: Eq[ButtonEvent] = _.isOn == _.isOn

  implicit val unit: Units[ButtonEvent] = "control event: The Button is pressed"
  implicit val dec: Decoder[ButtonEvent] = msg => msg.split("\n").collect{
    case s if s.startsWith("the-button:") => ButtonEvent(s.contains("1"))
  }
}
