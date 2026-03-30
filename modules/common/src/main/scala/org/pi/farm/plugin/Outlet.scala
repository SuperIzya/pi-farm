package org.pi.farm.plugin

import zio.json.ast.Json
import zio.json.*
import org.pi.farm.model.*

case class Outlet[Out: {JsonCodec, NotTuple}](name: Name, description: String, units: Units) {
  def format(value: Out): Json = Message.Data(value).toJsonAST.toOption.get
}

object Outlet {
  def apply[Out: {JsonCodec, NotTuple}](name: Name, units: Units): Outlet[Out] =
    new Outlet[Out](name, "", units)
}
