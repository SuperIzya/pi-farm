package org.pi.farm

import zio.json.{CamelCase, ExplicitEmptyCollections, JsonCodecConfiguration}

package object ws {
  given JsonCodecConfiguration = JsonCodecConfiguration
    .default
    .copy(
      sumTypeHandling = JsonCodecConfiguration.SumTypeHandling.WrapperWithClassNameField,
      fieldNameMapping = CamelCase,
      allowExtraFields = false,
      sumTypeMapping = CamelCase,
      explicitNulls = false,
      explicitEmptyCollections = ExplicitEmptyCollections(false, false),
      enumValuesAsStrings = true
    )
}
