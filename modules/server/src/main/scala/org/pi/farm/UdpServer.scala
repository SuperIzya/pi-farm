package org.pi.farm
import org.pi.farm.utils.ConfigCompanion
import zio.*
import zio.config.magnolia.deriveConfig

import java.net.{InetSocketAddress, StandardProtocolFamily, StandardSocketOptions}
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class UdpServer(config: UdpServer.Config, inbound: Enqueue[RawMessage], outbound: Dequeue[RawMessage]) {
  def start: URIO[Scope, Unit] = for {
    channel <- initChannel
    buffer = ByteBuffer.allocateDirect(bufferSize)
    _ <- ZIO.logInfo(s"UDP server is running on ${config.ip}:${config.port}")
    _ <- read(channel)
    _ <- write(channel)
  } yield ()

  private val bufferSize = 4 * 1024

  private def openChannel = for {
    channel <- ZIO.attempt(DatagramChannel.open(StandardProtocolFamily.INET))
    _       <- ZIO.fail(new RuntimeException("Failed to open the server")).unless(channel.isOpen)
    _       <- ZIO.attempt(channel.setOption(StandardSocketOptions.SO_RCVBUF, bufferSize))
    _       <- ZIO.attempt(channel.setOption(StandardSocketOptions.SO_SNDBUF, bufferSize))
    _       <- ZIO.attempt(channel.bind(new java.net.InetSocketAddress(config.ip, config.port)))
  } yield channel

  private def closeChannel(channel: DatagramChannel) =
    ZIO.attempt(channel.close()).tapErrorCause(ZIO.logErrorCause("Failed to stop the server", _)).orDie

  private def initChannel =
    ZIO
      .acquireRelease(openChannel)(closeChannel)
      .tapErrorCause(ZIO.logErrorCause(s"Failed to start UDP server on ${config.ip}:${config.port}", _))
      .orDie

  private def read(channel: DatagramChannel) = {
    val buffer = ByteBuffer.allocateDirect(16 * bufferSize)
    buffer.clear()
    val action = for {
      address <- ZIO.attempt(channel.receive(buffer))
      _       <- ZIO.when(address != null) {
        buffer.flip()
        address match {
          case address: InetSocketAddress =>
            inbound.offer(RawMessage(address, String(buffer.array())))
          case _ => ZIO.fail(new RuntimeException(s"Unsupported address type: $address"))
        }
      }
    } yield ()

    action.forever
      .tapErrorCause(ZIO.logErrorCause("Error in UDP server loop", _))
      .retry(Schedule.forever)
      .forkScoped
  }

  private def write(channel: DatagramChannel) =
    outbound.take
      .flatMap { message =>
        ZIO.attempt(channel.send(ByteBuffer.wrap(message.data.getBytes), message.ipAddress))
      }
      .forever
      .tapErrorCause(ZIO.logErrorCause("Error in UDP server loop", _))
      .retry(Schedule.forever)
      .forkScoped
}

object UdpServer {
  type Env = UdpServer.Config & Scope

  case class Config(port: Int, ip: String, queueSize: Int)
  object Config extends ConfigCompanion[Config]("udp-server")

  def live: URLayer[Env, Queues] = ZLayer {
    for {
      config   <- ZIO.service[Config]
      inbound  <- Queue.sliding[RawMessage](config.queueSize)
      outbound <- Queue.sliding[RawMessage](config.queueSize)
      server = new UdpServer(config, inbound, outbound)
      _ <- server.start
    } yield Queues(inbound, outbound)
  }
}
