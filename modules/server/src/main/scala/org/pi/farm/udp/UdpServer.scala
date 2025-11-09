package org.pi.farm.udp

import io.netty.channel.{Channel, ChannelFuture}
import io.netty.util.concurrent.GenericFutureListener
import org.pi.farm.model.*
import org.pi.farm.model.given
import io.scalaland.chimney.dsl.*
import zio.*

class UdpServer(
  driver: Driver,
  incomingQueue: IncomingQueue,
  queues: Queues
) {
  def start: RIO[Scope, Unit] =
    for {
      channel <- driver.start
      _       <- incomingQueue.incomingStream
        .map(toRawMessage)
        .foreach(queues.newIncoming)
        .forkScoped
      _ <- queues.outgoingStream
        .map(toBinaryMessage)
        .foreach(send(channel, _).tapErrorCause(ZIO.logErrorCause("Error sending message", _)).ignore)
        .forkScoped
    } yield ()

  private def toRawMessage(msg: BinaryMessage): RawMessage =
    msg
      .into[RawMessage]
      .withFieldConst(_.ipAddress, IpAddress(msg.ipAddress))
      .withFieldComputed(_.data, msg => new String(msg.data.toArray))
      .transform

  private def toBinaryMessage(msg: RawMessage): BinaryMessage =
    msg
      .into[BinaryMessage]
      .withFieldConst(_.ipAddress, IpAddress.java(msg.ipAddress))
      .withFieldComputed(_.data, msg => Chunk.fromArray(msg.data.getBytes))
      .transform

  private def send(channel: Channel, message: BinaryMessage): Task[Unit] = {

    def writeToChannel(success: => Unit)(failure: Throwable => Unit) = {
      lazy val listener: GenericFutureListener[ChannelFuture] = (future: ChannelFuture) => {
        future.removeListener(listener)
        if (!future.isSuccess) {
          ZIO.logError(s"Error sending message to ${channel.remoteAddress()}: ${future.cause()}")
        }
      }
      channel.writeAndFlush(message).addListener(listener)
    }

    def exec(runtime: zio.Runtime[Any], action: UIO[Boolean]): Unit = Unsafe.unsafe { unsafe ?=>
      runtime.unsafe.run(action)
    }

    for {
      runtime <- ZIO.runtime[Any]
      promise <- Promise.make[Throwable, Unit]
      _ = writeToChannel(exec(runtime, promise.succeed(())))(t => exec(runtime, promise.fail(t)))
      _ <- promise.await
    } yield ()
  }
}

object UdpServer {
  type Env = UdpConfig

  def live: RLayer[Env, Queues] = ZLayer.makeSome[Env, Queues](
    driver,
    queues,
    ZLayer.fromFunction(new UdpServer(_, _, _)),
    start
  )

  def queues: URLayer[Env, Queues] = ZLayer {
    for {
      config   <- ZIO.service[UdpConfig]
      inbound  <- Queue.sliding[RawMessage](config.queueSize)
      outbound <- Queue.sliding[RawMessage](config.queueSize)
    } yield new Queues(inbound, outbound)
  }

  private def driver: URLayer[UdpConfig, IncomingQueue & Driver] = Driver.live

  private def start: RLayer[UdpServer, Unit] = ZLayer.scoped {
    ZIO.logInfo("Starting UDP server") *>
      ZIO.service[UdpServer].flatMap(_.start) *>
      ZIO.logInfo("UDP server started")
  }
}
