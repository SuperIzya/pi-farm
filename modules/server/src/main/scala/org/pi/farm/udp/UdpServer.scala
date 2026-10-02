package org.pi.farm.udp

import org.pi.farm.model.Types.*

import io.scalaland.chimney.dsl.*

import zio.*

import scala.language.implicitConversions

import io.netty.channel.{Channel, ChannelFuture}
import io.netty.channel.socket.DatagramPacket
import io.netty.util.concurrent.GenericFutureListener

class UdpServer(
  driver: Driver,
  incomingQueue: IncomingQueue,
  queues: Queues
) {
  def start: RIO[Scope, Unit] =
    for {
      channel <- driver.start
      _       <- incomingQueue
                   .incomingStream
                   .map(toRawMessage)
                   .foreach(queues.newIncoming)
                   .forkScoped
      _       <- queues
                   .outgoingStream
                   .map(toBinaryMessage)
                   .foreach(send(channel))
                   .forkScoped
    } yield ()

  private def toRawMessage(msg: BinaryMessage): RawMessage =
    msg
      .into[RawMessage]
      .withFieldComputed(_.ipAddress, _.ipAddress.wrap)
      .withFieldComputed(_.data, msg => new String(msg.data.toArray))
      .transform

  private def toBinaryMessage(msg: RawMessage): BinaryMessage =
    msg
      .into[BinaryMessage]
      .withFieldComputed(_.ipAddress, _.ipAddress.unwrap)
      .withFieldComputed(_.data, msg => Chunk.fromArray(msg.data.getBytes))
      .transform

  private def toDatagramPacket(msg: BinaryMessage): DatagramPacket =
    new DatagramPacket(
      io.netty.buffer.Unpooled.wrappedBuffer(msg.data.toArray),
      msg.ipAddress.unwrap
    )

  private def send(channel: Channel): BinaryMessage => UIO[Unit] = {

    def writeToChannel(message: BinaryMessage)(success: => Unit)(failure: Throwable => Unit) = {
      lazy val listener: GenericFutureListener[ChannelFuture] = (future: ChannelFuture) => {
        future.removeListener(listener)
        if (!future.isSuccess) failure(future.cause())
        else success
      }
      channel.writeAndFlush(toDatagramPacket(message)).addListener(listener)
    }

    def exec(action: UIO[Boolean])(using runtime: zio.Runtime[Any]): Unit = Unsafe.unsafe { unsafe ?=>
      runtime.unsafe.run(action)
    }
    message =>
      ZIO
        .runtime[Any]
        .flatMap { runtime =>
          given zio.Runtime[Any] = runtime
          for {
            promise <- Promise.make[Throwable, Unit]
            _        =
              writeToChannel(message)(exec(promise.succeed(())))(t => exec(promise.fail(t)))
            _       <- promise.await
          } yield ()
        }
        .tapErrorCause(ZIO.logErrorCause("Error sending message", _))
        .ignore
  }
}

object UdpServer {
  type Env = UdpConfig & Scope

  def live: RLayer[Env, Queues] = ZLayer.makeSome[Env, Queues](
    driver,
    queues,
    ZLayer.fromFunction(new UdpServer(_, _, _)),
    start
  )

  def queues: URLayer[UdpConfig, Queues] = ZLayer {
    for {
      config <- ZIO.service[UdpConfig]
      queues <- Queues.make(config.queueSize)
    } yield queues
  }

  private def driver: URLayer[UdpConfig & Scope, IncomingQueue & Driver] = Driver.live

  private def start: RLayer[UdpServer & Scope, Unit] = ZLayer {
    ZIO.logInfo("Starting UDP server") *>
      ZIO.service[UdpServer].flatMap(_.start) *>
      ZIO.logInfo("UDP server started")
  }
}
