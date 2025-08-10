package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class PeripheryType(
  id: PeripheryTypeId, // Unique identifier for the periphery type
  name: String,        // Name of the periphery type
  units: String,
  description: String,
  image: String,
  direction: PeripheryType.Direction
)

object PeripheryType {
  given JsonCodec[PeripheryType] = DeriveJsonCodec.gen[PeripheryType]

  case class New(
    name: String,
    units: String,
    description: String,
    image: String,
    direction: PeripheryType.Direction
  )
  object New {
    given JsonCodec[New] = DeriveJsonCodec.gen[New]
  }

  enum Direction {
    case In, Out, Both
  }

  object Direction {
    given JsonCodec[Direction] = JsonCodec[String].transformOrFail(
      fromString,
      toString
    )

    def fromString(value: String): Either[String, Direction] = value.toLowerCase match {
      case "in"   => Right(Direction.In)
      case "out"  => Right(Direction.Out)
      case "both" => Right(Direction.Both)
      case _      => Left(s"Invalid direction: $value")
    }

    def toString(direction: Direction): String = direction match {
      case Direction.In   => "in"
      case Direction.Out  => "out"
      case Direction.Both => "both"
    }
  }
}
