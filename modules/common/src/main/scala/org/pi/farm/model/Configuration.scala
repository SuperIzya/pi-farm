package org.pi.farm.model

import zio.json.ast.Json
import zio.json.{DeriveJsonCodec, JsonCodec}

case class Configuration(
  id: Int,
  inbound: Set[InboundId],
  outbound: Set[OutboundId],
  processingUnit: String,
  additional: Option[Json] = None
)

object Configuration {
  given JsonCodec[Configuration] = DeriveJsonCodec.gen[Configuration]
}
