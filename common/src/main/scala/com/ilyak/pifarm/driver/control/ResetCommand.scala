package com.ilyak.pifarm.driver.control

import com.ilyak.pifarm.{ Encoder, Units }

case object ResetCommand {
  implicit val unit: Units[ResetCommand.type] = "control command: reset"

  implicit val enc: Encoder[ResetCommand.type] = Encoder(_ => s"reset")
}
