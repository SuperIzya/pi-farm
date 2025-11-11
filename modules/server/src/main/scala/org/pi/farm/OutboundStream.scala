package org.pi.farm

import org.pi.farm.model.Message.Outbound
import org.pi.farm.model.Message
import org.pi.farm.udp.{Queues, RawMessage}
import zio.*
import zio.json.*

import java.nio.ByteBuffer
import org.pi.farm.runtime.*

class OutboundStream(responseHub: ResponseHub, outbound: Enqueue[RawMessage], controllers: Controllers) {

  def run: UIO[Unit] =
    responseHub.toStream
      .mapZIO(encode(_).tapErrorCause(ZIO.logErrorCause("Error in outbound stream", _)).exit)
      .collectSuccess
      .foreach(outbound.offer)

  private def encode(message: Outbound): Task[RawMessage] =
    controllers.getAddress(message.controllerId).flatMap {
      case Some(address) =>
        ZIO.succeed(
          RawMessage(
            ipAddress = address,
            data = message.toJson
          )
        )
      case None =>
        ZIO.fail(new NoSuchElementException(s"Controller with ID ${message.controllerId} not found"))
    }
}

object OutboundStream {
  type Env = Controllers & Queues & Scope & ResponseHub
  def live: URLayer[Env, Unit] = ZLayer {
    for {
      controllers <- ZIO.service[Controllers]
      queues      <- ZIO.service[Queues]
      hub         <- ZIO.service[ResponseHub]
      outboundStream = new OutboundStream(hub, queues.outbound, controllers)
      _ <- outboundStream.run.forkScoped
    } yield ()
  }
}
