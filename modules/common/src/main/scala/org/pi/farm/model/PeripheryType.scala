package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

/** Describes a type of periphery that a controller may have, such as a humidity sensor, temperature sensor, or
  * actuator. A periphery type defines the physical or logical interface characteristics shared by all peripheries of
  * this kind.
  *
  * @param id
  *   unique identifier for this periphery type
  * @param name
  *   human-readable name (e.g. "DHT22 Humidity Sensor")
  * @param units
  *   measurement units for values produced or consumed (e.g. "%", "°C", "rad")
  * @param `type`
  *   primitive data type of the value (e.g. "Float", "Boolean")
  * @param description
  *   detailed description of the periphery type
  * @param image
  *   data URL of an image representing this periphery type (e.g. `data:image/png;base64,...`)
  * @param direction
  *   whether this periphery produces data ([[Direction.Out]]), consumes data ([[Direction.In]]), or does both
  *   ([[Direction.Both]])
  */
case class PeripheryType(
  id: PeripheryTypeId,
  name: Name,
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
