package org.pi.farm.common.plugins.processors

import org.pi.farm.model.Message.{Ping, Pong}
import org.pi.farm.model.given
import org.pi.farm.plugin.{DataProcessor, Inlet, Outlet, Service}
import org.pi.farm.plugin.macros.processor

import zio.ZIO
import zio.json.{JsonCodec, JsonEncoder}

import scala.language.implicitConversions

import cats.effect.kernel.Par

object PingPong extends Service {

  val service: Service.Creator = ZIO.succeed {
    Service("Ping Pong Service") { signalStream =>
      signalStream.map {
        case Ping(controllerId) => Some(Pong(controllerId))
        case _                  => None
      }.collectSome
    }
  }

}
