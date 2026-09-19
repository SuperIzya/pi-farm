package org.pi.farm.ws

import org.pi.farm.model.{Controller, ControllerType, FlowConfiguration, Message, PeripheryType}
import org.pi.farm.model.Types.{*, given}

import zio.json.*
import zio.json.ast.Json

import cats.Show

sealed trait Command

object Command {
  given JsonDecoder[Command] = DeriveJsonDecoder.gen[Command]

  sealed trait Data[+A] {
    def data: A
  }

  case class PartialCommand(data: Partial) extends Command with Data[Partial]
  given Show[Command] with
    def show(c: Command): String = c match
      case PartialCommand(data)      => s"PartialCommand(${data.copy(data = "<truncated>")})"
      case SavePeripheryType(data)   => s"SavePeripheryType(${data.copy(image = "<truncated>")})"
      case UpdatePeripheryType(data) => s"UpdatePeripheryType(${data.copy(image = "<truncated>")})"
      case other                     => other.toString

  case class SavePeripheryType(data: PeripheryType.New)     extends Command with Data[PeripheryType.New]
  case class SaveControllerType(data: ControllerType.New)   extends Command with Data[ControllerType.New]
  case class UpdatePeripheryType(data: PeripheryType)       extends Command with Data[PeripheryType]
  case class UpdateControllerType(data: ControllerType)     extends Command with Data[ControllerType]
  case class SaveController(data: Controller.New)           extends Command with Data[Controller.New]
  case class UpdateController(data: Controller)             extends Command with Data[Controller]
  case class SaveConfiguration(data: FlowConfiguration.New) extends Command with Data[FlowConfiguration.New]
  case class UpdateConfiguration(data: FlowConfiguration)   extends Command with Data[FlowConfiguration]
  case class DeletePeripheryType(data: PeripheryTypeId)     extends Command with Data[PeripheryTypeId]
  case class DeleteControllerType(data: ControllerTypeId)   extends Command with Data[ControllerTypeId]
  case class DeleteController(data: ControllerId)           extends Command with Data[ControllerId]
  case class DeleteConfiguration(data: ConfigurationId)     extends Command with Data[ConfigurationId]

  case class DataPacketCommand(data: Message.DataPacket) extends Command with Data[Message.DataPacket]

  case object GetPeripheryTypes  extends Command
  case object GetControllerTypes extends Command
  case object GetControllers     extends Command
  case object GetConfigurations  extends Command
  case object GetProcessingUnits extends Command
}
