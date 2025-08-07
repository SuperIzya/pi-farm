package org.pi.farm.model

import PeripheryType.Direction
import zio.json.JsonDecoder
import zio.json.ast.{Json, JsonCursor}

import scala.scalajs.js

class NewPeripheryType(
  val name: String,
  val description: String,
  val image: String,
  val direction: Direction
) extends js.Object

object NewPeripheryType {
  given JsonDecoder[NewPeripheryType] = JsonDecoder[Json].mapOrFail{ json =>
    for {
      name <- json.get[Json.Str](JsonCursor.field("name") >>> JsonCursor.isString)
      description <- json.get[Json.Str](JsonCursor.field("description") >>> JsonCursor.isString)
      image <- json.get[Json.Str](JsonCursor.field("image") >>> JsonCursor.isString)
      directionStr <- json.get[Json.Str](JsonCursor.field("direction") >>> JsonCursor.isString)
      direction <- Direction.fromString(directionStr.value)
    } yield new NewPeripheryType(name.value, description.value, image.value, direction)
  }
}
