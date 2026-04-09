package org.pi.farm.model

import org.pi.farm.model.ProcessorDefinition.{InputConnection, OutputConnection}
import zio.Chunk
import zio.json.ast.Json
import zio.json.{DeriveJsonCodec, JsonCodec}

/** A definition of reusable program that reads sensor data, performs calculations, and emits results. A processing unit
  * is defined by its input and output signatures (units + primitive type) and is parameterised at runtime via
  * [[params]].
  *
  * Example: a thermostat unit might accept temperature in °C as a `Float` and emit an on/off `Boolean` flag together
  * with a valve angle in radians as a `Float`.
  *
  * @param name
  *   unique name identifying this processing unit
  * @param description
  *   human-readable explanation of what the unit computes
  * @param paramsSchema
  *   JSON Schema describing the structure and constraints of [[params]]
  * @param inbound
  *   ordered list of expected input channels with their units and types
  * @param outbound
  *   ordered list of produced output channels with their units and types
  */
case class ProcessorDefinition(
  name: Name,
  description: String,
  paramsSchema: Json,
  inbound: Chunk[InputConnection],
  outbound: Chunk[OutputConnection]
)

object ProcessorDefinition {

  /** Describes a single data channel of a [[ProcessingUnit]]. */
  sealed trait Connection {
    def name: Name
    def description: String
    def units: Units
    def `type`: String
    def direction: Direction
  }

  /** An input channel: data flowing into the processing unit.
    *
    * @param name
    *   user-friendly label for UI display
    * @param units
    *   measurement units of the incoming value (e.g. "°C")
    * @param `type`
    *   primitive type of the value (e.g. "Float")
    */
  case class InputConnection(name: Name, description: String, units: Units, `type`: String) extends Connection {
    val direction: Direction = Direction.In
  }

  /** An output channel: data produced by the processing unit.
    *
    * @param name
    *   user-friendly label for UI display
    * @param units
    *   measurement units of the outgoing value (e.g. "rad")
    * @param `type`
    *   primitive type of the value (e.g. "Float")
    */
  case class OutputConnection(name: Name, description: String, units: Units, `type`: String) extends Connection {
    val direction: Direction = Direction.Out
  }

  private given JsonCodec[InputConnection]  = DeriveJsonCodec.gen[InputConnection]
  private given JsonCodec[OutputConnection] = DeriveJsonCodec.gen[OutputConnection]
  given JsonCodec[ProcessorDefinition]      = DeriveJsonCodec.gen[ProcessorDefinition]

}
