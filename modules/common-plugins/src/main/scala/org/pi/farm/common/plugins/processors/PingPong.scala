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
import org.pi.farm.plugin.Service

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
