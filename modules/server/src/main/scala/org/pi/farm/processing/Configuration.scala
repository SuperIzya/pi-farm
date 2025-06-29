package org.pi.farm.processing

import org.pi.farm.common.ControllerId
import org.pi.farm.processing.ProcessingUnit.{Discovery, ErrorHandler, PingPong}
import zio.json.ast.Json
import zio.json.{DeriveJsonCodec, JsonCodec}

case class Configuration(
                          inbound: Set[ControllerId],
                          outbound: Set[ControllerId],
                          processingUnit: String,
                          additional: Option[Json] = None
                        )

object Configuration {
  given JsonCodec[Configuration] = DeriveJsonCodec.gen[Configuration]

  def default: List[Configuration] = List(
    Configuration(Set.empty, Set.empty, PingPong.name),
    Configuration(Set.empty, Set.empty, ErrorHandler.name),
    Configuration(Set.empty, Set.empty, Discovery.name),
  )
}
