package org.pi.farm.ws

import org.pi.farm.model
import zio.json.*

sealed trait Command

object Command {
  case class SavePeripheryType(data: model.PeripheryType.New) extends Command
  case class SaveControllerType(data: model.ControllerType.New) extends Command
  case class UpdatePeripheryType(data: model.PeripheryType) extends Command
  case class UpdateControllerType(data: model.ControllerType) extends Command
  case class SaveController(data: model.Controller.New) extends Command
  case class UpdateController(data: model.Controller) extends Command

  case object GetPeripheryTypes extends Command
  case object GetControllerTypes extends Command
  case object GetControllers extends Command


  //private given JsonDecoder[SavePeripheryType] = DeriveJsonDecoder.gen[SavePeripheryType]

  given JsonDecoder[Command] = DeriveJsonDecoder.gen[Command]
}
