package org.pi.farm.model

import zio.json.ast.Json
import zio.Chunk
import zio.json.{DeriveJsonCodec, JsonCodec}

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
  case class New(
    name: Name,
    description: String,
    inbound: Chunk[Address],
    outbound: Chunk[Address],
    processingUnit: String,
    additional: Json
  )

  given JsonCodec[Configuration] = DeriveJsonCodec.gen[Configuration]
}
