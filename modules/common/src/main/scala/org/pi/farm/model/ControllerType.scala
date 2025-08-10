package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class ControllerType(
  id: ControllerTypeId, // Unique identifier for the controller type
  name: String,         // Name of the controller
  description: String,
  code: String,
  periphery: Map[String, PeripheryType]
)

object ControllerType {
  case class New(
    name: String,         // Name of the controller
    description: String,
    code: String,
    periphery: Map[String, PeripheryType]
  )
  object New {
    given JsonCodec[New] = DeriveJsonCodec.gen[New]
  }

  given JsonCodec[ControllerType] = DeriveJsonCodec.gen[ControllerType]
}
