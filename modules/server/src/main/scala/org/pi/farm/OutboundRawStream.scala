package org.pi.farm

import org.pi.farm.model.Message
import org.pi.farm.model.Message.{Outbound, ServerUp}
import org.pi.farm.model.Types.*
import org.pi.farm.runtime.*
import org.pi.farm.udp.{Queues, RawMessage}

import zio.*
import zio.json.*
import zio.stream.ZStream

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import scala.language.implicitConversions

class OutboundRawStream(
  responses: ResponseStream,
  outbound: Enqueue[RawMessage],
  controllers: Controllers
) {

  def run: UIO[Unit] =
    responses
      .mapZIO(encode(_).tapErrorCause(ZIO.logErrorCause("Error in outbound stream", _)).exit)
      .collectSuccess
      .foreach(outbound.offer)

  private def encode(message: Outbound): Task[RawMessage] =
    message match {
      case m: Message.WithControllerId =>
        controllers.getAddress(m.controllerId).flatMap {
          case Some(address) =>
            ZIO.succeed(RawMessage(address.wrap, message.toJson))
          case None          =>
            ZIO.fail(new NoSuchElementException(s"Controller with ID ${m.controllerId} not found"))
        }
      case ServerUp                    =>
        ZIO.succeed(
          RawMessage(OutboundRawStream.broadcastAddress.wrap, message.toJson)
        )
    }
}

object OutboundRawStream {
  type Env = Controllers & Queues & Scope & ResponseHub
  def live: URLayer[Env, Unit] = ZLayer {
    for {
      controllers   <- ZIO.service[Controllers]
      queues        <- ZIO.service[Queues]
      hub           <- ZIO.service[ResponseHub]
      stream        <- hub.subscribe
      outboundStream = new OutboundRawStream(stream, queues.outbound, controllers)
      _             <- outboundStream.run.forkScoped
    } yield ()
  }

  final val broadcastAddress: InetSocketAddress = new InetSocketAddress("255.255.255.255", 0)
}
