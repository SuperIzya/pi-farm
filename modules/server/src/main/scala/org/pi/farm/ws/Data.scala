package org.pi.farm.ws

import org.pi.farm.model
import org.pi.farm.model.Process
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.service.FirmwareService

import zio.Chunk
import zio.json.{DeriveJsonCodec, DeriveJsonEncoder, JsonCodec, JsonEncoder}
import zio.json.JsonError.Message
import zio.json.ast.Json

sealed trait Data {
  type Inner
  def data: Inner
}

object Data {
  import model.Codec.given

  given JsonEncoder[Data] = DeriveJsonEncoder.gen

  def error(message: String): Data = Error(message)

  sealed trait TypedData[A] extends Data {
    type Inner = A
    def data: A
  }

  case class Error(data: String)                          extends TypedData[String]
  case class ProcessLogs(data: Process.Output)            extends TypedData[Process.Output]
  case class ProcessState(data: Process.Status)           extends TypedData[Process.Status]
  case class PartialData(data: Partial)                   extends TypedData[Partial]
  case class PeripheryType(data: model.PeripheryType)     extends TypedData[model.PeripheryType]
  case class PeripheryTypes(data: Chunk[model.PeripheryType])
      extends TypedData[Chunk[model.PeripheryType]]
  case class ControllerType(data: model.ControllerType)   extends TypedData[model.ControllerType]
  case class ControllerTypes(data: Chunk[model.ControllerType])
      extends TypedData[Chunk[model.ControllerType]]
  case class Controller(data: model.Controller)           extends TypedData[model.Controller]
  case class Controllers(data: Chunk[model.Controller])   extends TypedData[Chunk[model.Controller]]
  case class Configuration(data: model.FlowConfiguration) extends TypedData[model.FlowConfiguration]
  case class Configurations(data: Chunk[model.FlowConfiguration])
      extends TypedData[Chunk[model.FlowConfiguration]]
  case class ProcessingUnit(data: model.ProcessorDefinition)
      extends TypedData[model.ProcessorDefinition]
  case class ProcessingUnits(data: Chunk[model.ProcessorDefinition])
      extends TypedData[Chunk[model.ProcessorDefinition]]

  case class AppConfigurationData(data: model.AppConfig) extends TypedData[model.AppConfig]

  case class ExtractedData(data: Json) extends TypedData[Json]

  case class Measurements(data: model.Message.Measurements)
      extends TypedData[model.Message.Measurements]

  case class Command(data: model.Message.Command) extends TypedData[model.Message.Command]
}
