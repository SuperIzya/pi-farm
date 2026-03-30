package org.pi.farm.plugin

import org.pi.farm.model.{Name, Units, Message}
import zio.{Queue, UIO}
import zio.json.JsonCodec
import zio.json.ast.Json

case class Inlet[In: {JsonCodec, NotTuple}](name: Name, description: String, units: Units) {
  def parse(data: Json): Either[String, In] =
    data.as[Message.Data[In]].map(_.value)
}

object Inlet {
  def apply[In: {JsonCodec, NotTuple}](name: Name, units: Units): Inlet[In] =
    new Inlet[In](name, "", units)
}
