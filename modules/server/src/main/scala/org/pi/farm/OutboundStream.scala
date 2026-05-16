package org.pi.farm

import org.pi.farm.model.{IpAddress, Message, given}
import org.pi.farm.model.Message.Outbound
import org.pi.farm.runtime.*
import org.pi.farm.udp.{Queues, RawMessage}

import zio.*
import zio.json.*
import zio.stream.ZStream

import java.nio.ByteBuffer
import scala.language.implicitConversions

class OutboundStream(responseHub: ResponseQueue, outbound: Enqueue[RawMessage], controllers: Controllers) {

  def run: UIO[Unit] =
    ZStream
      .fromQueue(responseHub)
      .mapZIO(encode(_).tapErrorCause(ZIO.logErrorCause("Error in outbound stream", _)).exit)
      .collectSuccess
      .foreach(outbound.offer)

  private def encode(message: Outbound): Task[RawMessage] =
    controllers.getAddress(message.controllerId).flatMap {
      case Some(address) =>
        ZIO.succeed(RawMessage(IpAddress(address), message.toJson))
      case None          =>
        ZIO.fail(new NoSuchElementException(s"Controller with ID ${message.controllerId} not found"))
    }
}

object OutboundStream {
  type Env = Controllers & Queues & Scope & ResponseQueue
  def live: URLayer[Env, Unit] = ZLayer {
    for {
      controllers   <- ZIO.service[Controllers]
      queues        <- ZIO.service[Queues]
      hub           <- ZIO.service[ResponseQueue]
      outboundStream = new OutboundStream(hub, queues.outbound, controllers)
      _             <- outboundStream.run.forkScoped
    } yield ()
  }
}
