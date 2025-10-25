package org.pi.farm.ws

import org.pi.farm.model
import zio.json.JsonError.Message
import zio.json.{DeriveJsonEncoder, JsonEncoder}

sealed trait Data {
  type Inner
  def data: Inner
}

object Data {

  import Codecs.given
  given JsonEncoder[Data] = DeriveJsonEncoder.gen

  sealed trait TypedData[A] extends Data {
    type Inner = A
    def data: A
  }

  def error(message: String): Data = Error(message)

  case class Error(data: String)                               extends TypedData[String]
  case class PartialData(data: Partial)                        extends TypedData[Partial]
  case class PeripheryType(data: model.PeripheryType)          extends TypedData[model.PeripheryType]
  case class PeripheryTypes(data: List[model.PeripheryType])   extends TypedData[List[model.PeripheryType]]
  case class ControllerType(data: model.ControllerType)        extends TypedData[model.ControllerType]
  case class ControllerTypes(data: List[model.ControllerType]) extends TypedData[List[model.ControllerType]]
  case class Controller(data: model.Controller)                extends TypedData[model.Controller]
  case class Controllers(data: List[model.Controller])         extends TypedData[List[model.Controller]]
  case class Configuration(data: model.Configuration)          extends TypedData[model.Configuration]
  case class Configurations(data: List[model.Configuration])   extends TypedData[List[model.Configuration]]
}
