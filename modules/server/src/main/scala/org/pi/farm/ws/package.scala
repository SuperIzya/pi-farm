package org.pi.farm

import zio.json.{CamelCase, ExplicitEmptyCollections, JsonCodecConfiguration, KebabCase}

package object ws {
  given JsonCodecConfiguration = JsonCodecConfiguration
    .default
    .copy(
      sumTypeHandling = JsonCodecConfiguration.SumTypeHandling.WrapperWithClassNameField,
      fieldNameMapping = CamelCase,
      allowExtraFields = false,
      sumTypeMapping = KebabCase,
      explicitNulls = false,
      explicitEmptyCollections = ExplicitEmptyCollections(encoding = true, decoding = false),
      enumValuesAsStrings = true
    )
}
