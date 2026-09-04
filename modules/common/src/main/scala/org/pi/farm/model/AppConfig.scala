package org.pi.farm.model

import org.pi.farm.model.Types.Units

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class AppConfig(units: Set[Units])

object AppConfig {
  given JsonCodec[AppConfig] = DeriveJsonCodec.gen[AppConfig]
}
