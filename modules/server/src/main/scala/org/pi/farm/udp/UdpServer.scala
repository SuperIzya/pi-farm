package org.pi.farm.udp

import zio.*

class UdpServer(
  driver: Driver,
  incomingQueue: IncomingQueue,
  queues: Queues
) {
  private val bufferSize = 4 * 1024

  def start: RIO[Scope, Unit] = {
    for {
      _ <- driver.start
      _ <- incomingQueue.incomingStream
        .map(toRawMessage)
        .foreach(queues.newIncoming)
        .forkScoped
    } yield ()
  }

  private def toRawMessage(msg: IncomingMessage): RawMessage =
    RawMessage(msg.sender, new String(msg.data.toArray))
}

object UdpServer {
  type Env = UdpConfig

  def queues: URLayer[Env, Queues] = ZLayer {
    for {
      config   <- ZIO.service[UdpConfig]
      inbound  <- Queue.sliding[RawMessage](config.queueSize)
      outbound <- Queue.sliding[RawMessage](config.queueSize)
    } yield new Queues(inbound, outbound)
  }

  private def driver: URLayer[UdpConfig, IncomingQueue & Driver] = Driver.live

  private def start: RLayer[UdpServer, Unit] = ZLayer.scoped{
    ZIO.logInfo("Starting UDP server") *>
      ZIO.service[UdpServer].flatMap(_.start)
  }

  def live: RLayer[Env, Queues] = ZLayer.makeSome[Env, Queues](
    driver,
    queues,
    ZLayer.fromFunction(new UdpServer(_, _, _)),
    start
  )
}
