package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

/** A schema for a class of IoT controllers, capturing the hardware model (e.g. Arduino Uno, TI CC3220) along with the
  * set of peripheries wired to specific pins or port identifiers. All physical controllers of the same board+wiring
  * share one [[ControllerType]].
  *
  * @param id
  *   unique identifier for this controller type
  * @param name
  *   human-readable name of the controller model (e.g. "Arduino Uno Rev3")
  * @param description
  *   detailed description of the controller type
  * @param schema
  *   optional URL to a file containing the soldering/wiring schema for this controller type
  * @param code
  *   firmware or driver code associated with this controller type
  * @param peripheries
  *   mapping from pin/port identifier ([[PeripheryId]]) to the type of periphery attached there ([[PeripheryTypeId]]),
  *   e.g. `{"1-3" -> humidityTypeId, "4-6" -> tempTypeId}`
  */
case class ControllerType(
  id: ControllerTypeId,
  name: Name,
  description: String,
  schema: Option[String],
  code: String,
  peripheries: Map[PeripheryId, PeripheryTypeId]
)

object ControllerType {

  /** Data required to register a new controller type (without a system-assigned id). */
  case class New(
    name: Name,
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
