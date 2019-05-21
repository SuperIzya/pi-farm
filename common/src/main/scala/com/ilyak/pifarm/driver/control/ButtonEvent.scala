package com.ilyak.pifarm.driver.control

import cats.Eq
import com.ilyak.pifarm.{ Decoder, Measurement, Units }

case class ButtonEvent(value: Boolean) extends Measurement[Boolean]

object ButtonEvent {
  def apply(value: String): ButtonEvent = new ButtonEvent(value.contains("1"))

  val parse: String => ButtonEvent = ButtonEvent(_)

  implicit val eq: Eq[ButtonEvent] = _.value == _.value

  implicit val unit: Units[ButtonEvent] = "control event: The Button is pressed"
  implicit val dec: Decoder[ButtonEvent] = "the-button:" -> ButtonEvent.parse

}
