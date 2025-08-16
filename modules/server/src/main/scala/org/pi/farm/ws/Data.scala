package org.pi.farm.ws
import org.pi.farm.model
import zio.json.{CamelCase, ExplicitEmptyCollections, JsonCodecConfiguration}

sealed trait Data

object Data {
  case class PeripheryType(data: model.PeripheryType) extends Data

  private given JsonCodecConfiguration = JsonCodecConfiguration.default.copy(
    sumTypeHandling = JsonCodecConfiguration.SumTypeHandling.DiscriminatorField("type"),
    fieldNameMapping = CamelCase,
    allowExtraFields = false,
    sumTypeMapping = CamelCase,
    explicitNulls = false,
    explicitEmptyCollections = ExplicitEmptyCollections(false, false),
    enumValuesAsStrings = true
  )

}
