package org.pi.farm.ws

import org.pi.farm.model
import zio.json.{DeriveJsonEncoder, JsonEncoder}

sealed trait Data

object Data {

  import Codecs.given
  given JsonEncoder[Data] = DeriveJsonEncoder.gen

  sealed trait WithData[A] {
    def data: A
  }

  case class PartialData(data: Partial)                        extends Data with WithData[Partial]
  case class PeripheryType(data: model.PeripheryType)          extends Data with WithData[model.PeripheryType]
  case class PeripheryTypes(data: List[model.PeripheryType])   extends Data with WithData[List[model.PeripheryType]]
  case class ControllerType(data: model.ControllerType)        extends Data with WithData[model.ControllerType]
  case class ControllerTypes(data: List[model.ControllerType]) extends Data with WithData[List[model.ControllerType]]
  case class Controller(data: model.Controller)                extends Data with WithData[model.Controller]
  case class Controllers(data: List[model.Controller])         extends Data with WithData[List[model.Controller]]
  case class Configuration(data: model.Configuration)          extends Data with WithData[model.Configuration]
  case class Configurations(data: List[model.Configuration])   extends Data with WithData[List[model.Configuration]]
}
