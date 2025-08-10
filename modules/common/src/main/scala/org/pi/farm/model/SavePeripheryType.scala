package org.pi.farm.model

import org.pi.farm.model.PeripheryType.Direction
import zio.json.{DeriveJsonDecoder, JsonDecoder}


case class SavePeripheryType(
  id: Option[Int],
  name: String,
  units: String,
  description: String,
  image: String,
  direction: Direction
)

object SavePeripheryType {
  given JsonDecoder[SavePeripheryType] = DeriveJsonDecoder.gen
}
