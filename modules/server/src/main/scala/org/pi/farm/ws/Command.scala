package org.pi.farm.ws

import org.pi.farm.model
import org.pi.farm.model.{ConfigurationId, ControllerId, ControllerTypeId, PeripheryTypeId}
import zio.json.*

sealed trait Command

object Command {
  import Codecs.given
  given JsonDecoder[Command] = DeriveJsonDecoder.gen[Command]

  sealed trait Data[+A] {
    def data: A
  }

  case class PartialCommand(data: Partial)                      extends Command with Data[Partial]
  case class SavePeripheryType(data: model.PeripheryType.New)   extends Command with Data[model.PeripheryType.New]
  case class SaveControllerType(data: model.ControllerType.New) extends Command with Data[model.ControllerType.New]
  case class UpdatePeripheryType(data: model.PeripheryType)     extends Command with Data[model.PeripheryType]
  case class UpdateControllerType(data: model.ControllerType)   extends Command with Data[model.ControllerType]
  case class SaveController(data: model.Controller.New)         extends Command with Data[model.Controller.New]
  case class UpdateController(data: model.Controller)           extends Command with Data[model.Controller]
  case class DeletePeripheryType(data: PeripheryTypeId)         extends Command with Data[PeripheryTypeId]
  case class DeleteControllerType(data: ControllerTypeId)       extends Command with Data[ControllerTypeId]
  case class DeleteController(data: ControllerId)               extends Command with Data[ControllerId]
  case class DeleteConfiguration(data: ConfigurationId)         extends Command with Data[ConfigurationId]

  case object GetPeripheryTypes  extends Command
  case object GetControllerTypes extends Command
  case object GetControllers     extends Command
  case object GetConfigurations  extends Command
}
