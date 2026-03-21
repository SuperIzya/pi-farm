package org.pi.farm.model

import zio.json.ast.Json
import zio.Chunk
import zio.json.{DeriveJsonCodec, JsonCodec}

/** A live wiring of a [[ProcessingUnit]] to specific controller peripheries. A configuration binds a named processing
  * unit to concrete data sources (inbound addresses) and data sinks (outbound addresses), forming a complete data-flow
  * pipeline.
  *
  * Example: bind a thermostat [[ProcessingUnit]] so that it reads from the temperature sensor on controller #5, pin
  * "4-6", and writes its on/off result to the relay on controller #7, pin "1".
  *
  * @param id
  *   unique identifier for this configuration
  * @param name
  *   human-readable label for this pipeline
  * @param description
  *   notes on purpose or placement
  * @param inbound
  *   ordered list of [[Address]]es supplying data to the processing unit; must match the unit's
  *   [[ProcessingUnit.inbound]] channel list in order
  * @param outbound
  *   ordered list of [[Address]]es that receive the processing unit's output; must match the unit's
  *   [[ProcessingUnit.outbound]] channel list in order
  * @param processingUnit
  *   name of the [[ProcessingUnit]] to execute
  * @param additional
  *   arbitrary extra configuration passed to the processing unit at runtime
  */
case class Configuration(
  id: ConfigurationId,
  name: Name,
  description: String,
  inbound: Chunk[Address],
  outbound: Chunk[Address],
  processingUnit: String,
  additional: Json
)

object Configuration {

  /** Data required to create a new configuration (without a system-assigned id). */
  case class New(
    name: Name,
    description: String,
    inbound: Chunk[Address],
    outbound: Chunk[Address],
    processingUnit: String,
    additional: Json
  )

  given JsonCodec[Configuration]     = DeriveJsonCodec.gen[Configuration]
  given JsonCodec[Configuration.New] = DeriveJsonCodec.gen[Configuration.New]
}
