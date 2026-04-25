package org.pi.farm.model

import zio.{Chunk, NonEmptyChunk}
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
  description: String,
  image: String,
  connections: NonEmptyChunk[PeripheryType.Connection]
) {
  val connectionsMap: Map[Name, PeripheryType.Connection] = connections.map(c => c.name -> c).toMap
}

object PeripheryType {
  given JsonCodec[PeripheryType] = DeriveJsonCodec.gen[PeripheryType]

  /** A single named connection point on a periphery, representing one data channel that can send or receive values. A
    * periphery type can have multiple connections — e.g. a solenoid motor might have an outbound "current" connection,
    * an outbound "angle" connection, and an inbound "command" connection.
    *
    * @param name
    *   human-readable name identifying this connection within its periphery (e.g. "current", "angle", "command")
    * @param direction
    *   whether this connection produces data ([[Direction.Out]]), consumes data ([[Direction.In]]), or does both
    *   ([[Direction.Both]])
    * @param units
    *   measurement units for values on this connection (e.g. "A", "°", "rpm")
    * @param `type`
    *   primitive data type of the value (e.g. "Float", "Boolean", "Int")
    */
  case class Connection(
    name: Name,
    direction: Direction,
    units: Units,
    `type`: String
  )
  object Connection {
    given JsonCodec[Connection] = DeriveJsonCodec.gen[Connection]
  }

  case class New(
    name: Name,
    description: String,
    image: String,
    connections: NonEmptyChunk[Connection]
  )
  object New {
    given JsonCodec[New] = DeriveJsonCodec.gen[New]
  }

}
