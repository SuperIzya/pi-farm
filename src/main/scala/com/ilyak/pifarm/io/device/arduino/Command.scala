package com.ilyak.pifarm.io.device.arduino

import com.ilyak.pifarm.Units
import com.ilyak.pifarm.Encoder
import play.api.libs.json.{ Json, OFormat }

case class Command(led: Boolean)

object Command {
  implicit val unit: Units[Command] = "default command"

  implicit val enc: Encoder[Command] = c => if (c.led) "1" else "0"

  implicit val format: OFormat[Command] = Json.format[Command]
}

