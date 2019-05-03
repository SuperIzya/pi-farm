package com.ilyak.pifarm.driver.control

import com.ilyak.pifarm.{ Encoder, Units }

case class LedCommand(led: Boolean)

object LedCommand {
  implicit val unit: Units[LedCommand] = "control command: led"

  implicit val enc: Encoder[LedCommand] = c => if (c.led) "1" else "0"

}

