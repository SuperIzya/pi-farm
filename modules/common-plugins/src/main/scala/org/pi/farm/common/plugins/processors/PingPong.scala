package org.pi.farm.common.plugins.processors

import org.pi.farm.plugin.macros.processor
import org.pi.farm.plugin.{DataProcessor, Inlet, Outlet}
import org.pi.farm.model.given
import org.pi.farm.model.Message.Ping
import org.pi.farm.model.Message.Pong
import zio.ZIO
import zio.json.JsonCodec
import scala.language.implicitConversions
import cats.effect.kernel.Par
import zio.json.JsonEncoder

@processor(name = "PingPong", description = "A simple processor that responds to Ping messages with Pong messages")
object PingPong extends DataProcessor {

  type ParamsType = DataProcessor.NoParams
  given paramsCodec: JsonCodec[ParamsType] = DataProcessor.noParamsCodec

  val inlet  = Inlet[Ping]("Ping Inlet", "Receives Ping messages", "Ping")
  val outlet = Outlet[Pong]("Pong Outlet", "Sends Pong messages", "Pong")
  val work   = from(inlet).to(outlet).via { ping =>
    Pong(ping.controllerId)
  }
}
