package org.pi.farm.model

import zio.json.JsonCodec

case class PeripheryType(
  id: PeripheryTypeId, // Unique identifier for the periphery type
  units: String = "",
  description: String = "",
  picture: String = "",
  direction: PeripheryType.Direction
)

object PeripheryType {
  enum Direction {
    case In, Out, Both
  }

  object Direction {
    def fromString(value: String): Either[String, Direction] = value.toLowerCase match {
        case "in"  => Right(Direction.In)
        case "out" => Right(Direction.Out)
        case "both" => Right(Direction.Both)
        case _     => Left(s"Invalid direction: $value")
      }

    def toString(direction: Direction): String = direction match {
      case Direction.In => "in"
      case Direction.Out => "out"
      case Direction.Both => "both"
    }

    given JsonCodec[Direction] = JsonCodec[String].transformOrFail(
      fromString,
      toString
    )
  }

}
