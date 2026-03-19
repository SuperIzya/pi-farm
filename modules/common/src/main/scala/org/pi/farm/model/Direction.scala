package org.pi.farm.model

import zio.json.JsonCodec

enum Direction {
  case In, Out, Both
}

object Direction {
  given JsonCodec[Direction] = JsonCodec[String].transformOrFail(
    fromString,
    toString
  )

  def fromString(value: String): Either[String, Direction] = value.toLowerCase match {
    case "in"   => Right(In)
    case "out"  => Right(Out)
    case "both" => Right(Both)
    case _      => Left(s"Invalid direction: $value")
  }

  def toString(direction: Direction): String = direction match {
    case In   => "in"
    case Out  => "out"
    case Both => "both"
  }
}
