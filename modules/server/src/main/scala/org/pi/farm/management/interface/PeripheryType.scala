package org.pi.farm.management.interface
import org.pi.farm.common.PeripheryType.Direction
import zio.json.{DeriveJsonEncoder, JsonEncoder}

case class PeripheryType(
  id: Int,
  name: String,
  description: String,
  units: String,
  direction: Direction,
  picture: String = ""
)

object PeripheryType {
  given JsonEncoder[PeripheryType] = DeriveJsonEncoder.gen[PeripheryType]

}
