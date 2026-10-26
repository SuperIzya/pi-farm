package org.pi.farm.model

import zio.json.{JsonCodecConfiguration, KebabCase}

object Codec {
  given JsonCodecConfiguration = JsonCodecConfiguration
    .default
    .copy(
      fieldNameMapping = KebabCase,
      sumTypeMapping = KebabCase
    )
}
