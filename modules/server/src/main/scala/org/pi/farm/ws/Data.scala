package org.pi.farm.ws

import org.pi.farm.model
import zio.json.CamelCase
import zio.json.DeriveJsonEncoder
import zio.json.ExplicitEmptyCollections
import zio.json.JsonCodecConfiguration
import zio.json.JsonEncoder

sealed trait Data

object Data {
  sealed trait WithData[A] extends Data {
    def data: A
  }
  case class PeripheryType(data: model.PeripheryType)          extends WithData[model.PeripheryType]
  case class PeripheryTypes(data: List[model.PeripheryType])   extends WithData[List[model.PeripheryType]]
  case class ControllerType(data: model.ControllerType)        extends WithData[model.ControllerType]
  case class ControllerTypes(data: List[model.ControllerType]) extends WithData[List[model.ControllerType]]
  case class Controller(data: model.Controller)                extends WithData[model.Controller]
  case class Controllers(data: List[model.Controller])         extends WithData[List[model.Controller]]
  case class Configuration(data: model.Configuration)          extends WithData[model.Configuration]
  case class Configurations(data: List[model.Configuration])   extends WithData[List[model.Configuration]]

  given JsonEncoder[Data] = DeriveJsonEncoder.gen

}
