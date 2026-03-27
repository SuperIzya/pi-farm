package org.pi.farm.plugin

import izumi.reflect.Tag
import zio.json._
import zio.json.ast.Json
import org.pi.farm.model.Message.{DataPacket, Outbound}
import org.pi.farm.model.*
import org.pi.farm.model.given
import zio.Chunk
import scala.language.implicitConversions
import cats.effect.kernel.Sync.Type
import scala.util.NotGiven

case class Outlet[Out](name: String, description: String, units: String)

object Outlet {
  def apply[Out](name: String, units: String): Outlet[Out] =
    new Outlet[Out](name, "", units)
}
