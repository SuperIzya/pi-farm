package org.pi.farm

import zio.json.{ExplicitEmptyCollections, JsonCodecConfiguration, SnakeCase}

package object ws {

  given JsonCodecConfiguration = JsonCodecConfiguration.default.copy(
    sumTypeHandling = JsonCodecConfiguration.SumTypeHandling.DiscriminatorField("command"),
    fieldNameMapping = SnakeCase,
    allowExtraFields = false,
    sumTypeMapping = SnakeCase,
    explicitNulls = false,
    explicitEmptyCollections = ExplicitEmptyCollections(false, false),
    enumValuesAsStrings = true
  )
}
