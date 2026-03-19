package org.pi.farm.ws

import org.pi.farm.model
import zio.Chunk
import zio.json.JsonError.Message
import zio.json.{DeriveJsonEncoder, JsonEncoder}

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

  case class Error(data: String)                                extends TypedData[String]
  case class PartialData(data: Partial)                         extends TypedData[Partial]
  case class PeripheryType(data: model.PeripheryType)           extends TypedData[model.PeripheryType]
  case class PeripheryTypes(data: Chunk[model.PeripheryType])   extends TypedData[Chunk[model.PeripheryType]]
  case class ControllerType(data: model.ControllerType)         extends TypedData[model.ControllerType]
  case class ControllerTypes(data: Chunk[model.ControllerType]) extends TypedData[Chunk[model.ControllerType]]
  case class Controller(data: model.Controller)                 extends TypedData[model.Controller]
  case class Controllers(data: Chunk[model.Controller])         extends TypedData[Chunk[model.Controller]]
  case class Configuration(data: model.Configuration)           extends TypedData[model.Configuration]
  case class Configurations(data: Chunk[model.Configuration])   extends TypedData[Chunk[model.Configuration]]
  case class ProcessingUnit(data: model.ProcessingUnit)         extends TypedData[model.ProcessingUnit]
  case class ProcessingUnits(data: Chunk[model.ProcessingUnit]) extends TypedData[Chunk[model.ProcessingUnit]]
}
