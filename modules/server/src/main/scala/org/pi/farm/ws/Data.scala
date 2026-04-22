package org.pi.farm.ws

import org.pi.farm.model

import zio.Chunk
import zio.json.{DeriveJsonEncoder, JsonEncoder}
import zio.json.JsonError.Message

sealed trait Data {
  type Inner
  def data: Inner
}

object Data {

  given JsonEncoder[Data] = DeriveJsonEncoder.gen

  def error(message: String): Data = Error(message)

  sealed trait TypedData[A] extends Data {
    type Inner = A
    def data: A
  }

  case class Error(data: String)                                     extends TypedData[String]
  case class PartialData(data: Partial)                              extends TypedData[Partial]
  case class PeripheryType(data: model.PeripheryType)                extends TypedData[model.PeripheryType]
  case class PeripheryTypes(data: Chunk[model.PeripheryType])        extends TypedData[Chunk[model.PeripheryType]]
  case class ControllerType(data: model.ControllerType)              extends TypedData[model.ControllerType]
  case class ControllerTypes(data: Chunk[model.ControllerType])      extends TypedData[Chunk[model.ControllerType]]
  case class Controller(data: model.Controller)                      extends TypedData[model.Controller]
  case class Controllers(data: Chunk[model.Controller])              extends TypedData[Chunk[model.Controller]]
  case class Configuration(data: model.FlowConfiguration)            extends TypedData[model.FlowConfiguration]
  case class Configurations(data: Chunk[model.FlowConfiguration])    extends TypedData[Chunk[model.FlowConfiguration]]
  case class ProcessingUnit(data: model.ProcessorDefinition)         extends TypedData[model.ProcessorDefinition]
  case class ProcessingUnits(data: Chunk[model.ProcessorDefinition]) extends TypedData[Chunk[model.ProcessorDefinition]]
}
