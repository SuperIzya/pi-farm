package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class ControllerType(
  id: ControllerTypeId, // Unique identifier for the controller type
  name: String,         // Name of the controller
  description: String,
  schema: Option[String],
  code: String,
  peripheries: Map[PeripheryId, PeripheryTypeId]
)

object ControllerType {
  case class New(
    name: String, // Name of the controller
    description: String,
    schema: Option[String],
    code: String,
    peripheries: Map[PeripheryId, PeripheryTypeId]
  )
  object New {
    given JsonCodec[New] = DeriveJsonCodec.gen[New]
  }
  given JsonCodec[ControllerType] = DeriveJsonCodec.gen[ControllerType]
}
