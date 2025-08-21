package org.pi.farm.ws

import org.pi.farm.model
import org.pi.farm.model.{ConfigurationId, ControllerId, ControllerTypeId, PeripheryTypeId}
import zio.json.*

sealed trait Command

object Command {
  given JsonDecoder[Command] = DeriveJsonDecoder.gen[Command]

  sealed trait WithData[A] extends Command {
    def data: A
  }

  case class SavePeripheryType(data: model.PeripheryType.New)   extends WithData[model.PeripheryType.New]
  case class SaveControllerType(data: model.ControllerType.New) extends WithData[model.ControllerType.New]
  case class UpdatePeripheryType(data: model.PeripheryType)     extends WithData[model.PeripheryType]
  case class UpdateControllerType(data: model.ControllerType)   extends WithData[model.ControllerType]
  case class SaveController(data: model.Controller.New)         extends WithData[model.Controller.New]
  case class UpdateController(data: model.Controller)           extends WithData[model.Controller]
  case class DeletePeripheryType(data: PeripheryTypeId)         extends WithData[PeripheryTypeId]
  case class DeleteControllerType(data: ControllerTypeId)       extends WithData[ControllerTypeId]
  case class DeleteController(data: ControllerId)               extends WithData[ControllerId]
  case class DeleteConfiguration(data: ConfigurationId)         extends WithData[ConfigurationId]

  case object GetPeripheryTypes  extends Command
  case object GetControllerTypes extends Command
  case object GetControllers     extends Command
}
