package org.pi.farm

import zio.json.{CamelCase, KebabCase, ExplicitEmptyCollections, JsonCodecConfiguration}

package object ws {
  given JsonCodecConfiguration = JsonCodecConfiguration.default
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
