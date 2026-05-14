package org.pi.farm.model

import org.pi.farm.plugin.syntax.Flow

import zio.{Chunk, NonEmptyChunk}
import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}
import zio.json.ast.Json

import scala.collection.immutable.SortedSet

import cats.data.NonEmptySet
import cats.kernel.Order

/** A live wiring of one or more [[DataProcessor]]s to specific controller peripheries. A configuration groups related
  * processors into a single deployable pipeline that can be started and stopped as a unit.
  *
  * Each [[Processor]] within the configuration binds a named processing unit to concrete data sources (inbound
  * addresses) and data sinks (outbound addresses), forming a complete data-flow path.
  *
  * Example: a greenhouse configuration might contain a thermostat processor reading from a temperature sensor on
  * controller #5 pin "4-6" and writing to a relay on controller #7 pin "1", alongside a humidity processor reading from
  * a humidity sensor and controlling a valve.
  *
  * @param id
  *   unique identifier for this configuration
  * @param name
  *   human-readable label for this pipeline
  * @param description
  *   notes on purpose or placement
  * @param processors
  *   set of [[Processor]] definitions that make up this pipeline; each binds a processing unit to its own inbound and
  *   outbound addresses
  */
case class FlowConfiguration(
  id: ConfigurationId,
  name: Name,
  description: String,
  processors: NonEmptySet[FlowConfiguration.Processor]
)

object FlowConfiguration {

  /** A single processing unit wired to its data sources and sinks within a configuration.
    *
    * @param unit
    *   name of the [[DataProcessor]] to execute (must match a registered processor manifest)
    * @param parameters
    *   arbitrary JSON configuration passed to the processing unit at runtime
    * @param inbound
    *   ordered list of [[Address]]es supplying data to this processor; must match the processor's inlet channel list in
    *   order
    * @param outbound
    *   ordered list of [[Address]]es that receive this processor's output; must match the processor's outlet channel
    *   list in order
    */
  case class Processor(
    unit: String,
    parameters: Json,
    inbound: Chunk[Address],
    outbound: Chunk[Address]
  )

  /** Data required to create a new configuration (without a system-assigned id). */
  case class New(
    name: Name,
    description: String,
    processors: NonEmptySet[FlowConfiguration.Processor]
  )

  given Order[Processor]    = Order.by[Processor, String](_.unit)
  given Ordering[Processor] = Ordering.by[Processor, String](_.unit)

  given [T: JsonEncoder] => JsonEncoder[NonEmptySet[T]] = JsonEncoder
    .nonEmptyChunk[T]
    .contramap[NonEmptySet[T]](nes => NonEmptyChunk(nes.head, nes.tail.toSeq*))

  given [T: {JsonDecoder, Order}] => JsonDecoder[NonEmptySet[T]] = JsonDecoder
    .nonEmptyChunk[T]
    .map(c => NonEmptySet.of(c.head, c.tail.toSeq*))

  given JsonCodec[Address]               = DeriveJsonCodec.gen[Address]
  given JsonCodec[Processor]             = DeriveJsonCodec.gen[Processor]
  given JsonCodec[FlowConfiguration]     = DeriveJsonCodec.gen[FlowConfiguration]
  given JsonCodec[FlowConfiguration.New] = DeriveJsonCodec.gen[FlowConfiguration.New]

  given [T] => Conversion[NonEmptySet[T], Set[T]]           = _.toSortedSet
  given [T: Ordering] => Conversion[Set[T], NonEmptySet[T]] = s => NonEmptySet.fromSetUnsafe(SortedSet.from(s))
}
