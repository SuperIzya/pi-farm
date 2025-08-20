package org.pi.farm.ws

import org.pi.farm.model
import zio.json.CamelCase
import zio.json.DeriveJsonEncoder
import zio.json.ExplicitEmptyCollections
import zio.json.JsonCodecConfiguration
import zio.json.JsonEncoder

sealed trait Data

object Data {
  case class PeripheryType(data: model.PeripheryType)          extends Data
  case class PeripheryTypes(data: List[model.PeripheryType])   extends Data
  case class ControllerType(data: model.ControllerType)        extends Data
  case class ControllerTypes(data: List[model.ControllerType]) extends Data
  case class Controller(data: model.Controller)                extends Data
  case class Controllers(data: List[model.Controller])         extends Data
  case class Configuration(data: model.Configuration)          extends Data
  case class Configurations(data: List[model.Configuration])   extends Data

  given JsonEncoder[Data] = DeriveJsonEncoder.gen

}
