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
  private given JsonEncoder[Direction] = JsonEncoder[String].contramap {
    case Direction.In   => "in"
    case Direction.Out  => "out"
    case Direction.Both => "both"
  }
  given JsonEncoder[PeripheryType] = DeriveJsonEncoder.gen[PeripheryType]

}
