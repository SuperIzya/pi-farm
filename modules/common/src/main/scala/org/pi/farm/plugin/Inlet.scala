package org.pi.farm.plugin

import org.pi.farm.model.{*, given}
import zio.json.JsonCodec
import zio.json.ast.Json

import scala.language.implicitConversions

case class Inlet[In: {JsonCodec, NotTuple}](name: Name, description: String, units: Units) {
  def parse(data: Json): Either[String, In] =
    data.as[Message.Data[In]].map(_.value)
}

object Inlet {
  def apply[In: {JsonCodec, NotTuple}](name: String, units: String): Inlet[In] =
    new Inlet[In](name, "", units)

}
