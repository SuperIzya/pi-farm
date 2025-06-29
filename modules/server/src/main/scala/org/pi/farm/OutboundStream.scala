package org.pi.farm

import org.pi.farm.common.Message.Outbound
import org.pi.farm.common.{Message, MessageHeader}
import zio.*

import java.nio.ByteBuffer

class OutboundStream(responseHub: ResponseHub, outbound: Enqueue[RawMessage], controllers: Controllers) {

  def run: UIO[Unit] =
    responseHub.toStream
      .mapZIO(encode(_).tapErrorCause(ZIO.logErrorCause("Error in outbound stream", _)).exit)
      .collectSuccess
      .foreach(outbound.offer)


  private def encode(message: Outbound): Task[RawMessage] =
    controllers.getAddress(message.controllerId).flatMap {
      case Some(address) =>
        message match {
          case Message.Command(_, dataPoints) =>
            val buffer = ByteBuffer.allocate(12 * dataPoints.size + 16)
            buffer.put(MessageHeader[Message.Command].header)
            dataPoints.foreach { dataPoint =>
              buffer.putInt(dataPoint.peripheryId)
              buffer.putDouble(dataPoint.value)
            }
            ZIO.succeed(RawMessage(address, buffer.flip()))
          case _: Message.ServerDiscovered =>
            val buffer = ByteBuffer.allocate(1) // Adjust size as needed
            buffer.put(MessageHeader[Message.ServerDiscovered].header)
            ZIO.succeed(RawMessage(address, buffer.flip()))
          case _: Message.Pong =>
            val buffer = ByteBuffer.allocate(1) // Adjust size as needed
            buffer.put(MessageHeader[Message.Pong].header)
            ZIO.succeed(RawMessage(address, buffer.flip()))
        }
      case None =>
        ZIO.fail(new NoSuchElementException(s"Controller with ID ${message.controllerId} not found"))
    }
}

object OutboundStream {
  type Env = Controllers & Queues & Scope & ResponseHub
  def live: URLayer[Env, Unit] = ZLayer{
    for {
      controllers <- ZIO.service[Controllers]
      queues <- ZIO.service[Queues]
      hub <- ZIO.service[ResponseHub]
      outboundStream = new OutboundStream(hub, queues.outbound, controllers)
      _ <- outboundStream.run.forkScoped
    } yield ()
  }
}
