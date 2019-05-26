package com.ilyak.pifarm.driver.control

import com.ilyak.pifarm.{ Command, Encoder, Units }

case class LedCommand(led: Boolean) extends Command("the-led")

object LedCommand {
  implicit val unit: Units[LedCommand] = "control command: led"

  implicit val enc: Encoder[LedCommand] = Encoder(c => s"the-led: ${if (c.led) "1" else "0"}")

}

