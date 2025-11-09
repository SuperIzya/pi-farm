package org.pi.farm.model

import zio.json.ast.Json
import zio.Chunk
import zio.json.{DeriveJsonCodec, JsonCodec}

case class Configuration(
  id: Int,
  inbound: Chunk[Address],
  outbound: Chunk[Address],
  processingUnit: String,
  additional: Option[Json] = None
)

object Configuration {

  given JsonCodec[Configuration] = DeriveJsonCodec.gen[Configuration]
}
