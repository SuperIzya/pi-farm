package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class PeripheryType(
  id: PeripheryTypeId, // Unique identifier for the periphery type
  name: Name,          // Name of the periphery type
  units: Units,
  `type`: String,
  description: String,
  image: String,
  direction: Direction
)

object PeripheryType {
  given JsonCodec[PeripheryType] = DeriveJsonCodec.gen[PeripheryType]

  case class New(
    name: Name,
    units: Units,
    `type`: String,
    description: String,
    image: String,
    direction: Direction
  )
  object New {
    given JsonCodec[New] = DeriveJsonCodec.gen[New]
  }

}
