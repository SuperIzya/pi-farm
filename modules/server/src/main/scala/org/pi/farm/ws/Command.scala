package org.pi.farm.ws

import org.pi.farm.model
import zio.json.*

sealed trait Command

object Command {
  case class SavePeripheryType(data: model.SavePeripheryType) extends Command
  case class SaveControllerType(data: model.SaveControllerType) extends Command



  //private given JsonDecoder[SavePeripheryType] = DeriveJsonDecoder.gen[SavePeripheryType]

  given JsonDecoder[Command] = DeriveJsonDecoder.gen[Command]
}
