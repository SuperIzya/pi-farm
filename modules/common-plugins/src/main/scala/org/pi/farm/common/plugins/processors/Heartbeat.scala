package org.pi.farm.common.plugins.processors

import org.pi.farm.model.Message.{Outbound, Ping, Pong}
import org.pi.farm.model.given
import org.pi.farm.plugin.Service
import org.pi.farm.runtime.{Controllers, Environment, ResponseHub}

import zio.{Queue, RIO, ZIO}
import zio.json._
import zio.json.ast.Json
import zio.stream.{Take, ZStream}

import scala.language.implicitConversions

object Heartbeat extends Service {
  val service: Service.Creator =
    for {
      controllers <- ZIO.service[Controllers]
      outgoing    <- ZIO.service[ResponseHub]
    } yield Service("Heartbeat") { signalStream =>
      for {
        queue         <- Queue.bounded[Outbound](1)
        controllerIds <- controllers.listControllerIds
        _             <- ZIO.foreachDiscard(controllerIds) { id => queue.offer(Pong(id)) }
      } yield ZStream.fromQueue(queue)
    }
}
