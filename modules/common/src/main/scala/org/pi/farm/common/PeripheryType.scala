package org.pi.farm.common

import zio.json.JsonEncoder

case class PeripheryType(
  id: PeripheryTypeId, // Unique identifier for the periphery type
  units: String = "",
  description: String = "",
  picture: String = "",
  direction: PeripheryType.Direction
)

object PeripheryType {
  enum Direction:
    case In, Out, Both
}
