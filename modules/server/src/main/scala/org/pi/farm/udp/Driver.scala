package org.pi.farm.udp

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.util.ResourceLeakDetector
import zio.*
import zio.http.netty.server.ServerEventLoopGroups
import zio.http.netty.{ChannelType, NettyConfig, NettyFutureExecutor}

class Driver(
  config: UdpConfig,
  nettyConfig: NettyConfig,
  channelInitializer: UdpChannelHandler,
  eventLoopGroups: ServerEventLoopGroups
) {
  def start: RIO[Scope, Unit] = {
    for {
      chf <- ZIO.attempt {
        new Bootstrap()
          .group(eventLoopGroups.boss)
          .channel(classOf[NioDatagramChannel])
          .option(ChannelOption.AUTO_CLOSE, true)
          .option(ChannelOption.SO_BROADCAST, true)
          .handler(channelInitializer)
          .bind(config.port)
          .sync()
      }
      _       <- NettyFutureExecutor.scoped(chf)
      _       <- ZIO.succeed(ResourceLeakDetector.setLevel(nettyConfig.leakDetectionLevel.toNetty))
      channel <- ZIO.attempt(chf.channel())
      _       <- Scope.addFinalizer(NettyFutureExecutor.executed(channel.close()).ignoreLogged)
    } yield ()
  }
}

object Driver {

  private val nettyConfig = NettyConfig.default
    .channelType(ChannelType.NIO)
    .maxThreads(2)
    .bossGroup(NettyConfig.default.bossGroup.copy(channelType = ChannelType.NIO, nThreads = 2))

  def live: URLayer[UdpConfig, Driver & IncomingQueue] = ZLayer.makeSome[UdpConfig, Driver & IncomingQueue](
    ZLayer.succeed(nettyConfig),
    ServerEventLoopGroups.live,
    IncomingQueue.live,
    UdpChannelHandler.live,
    ZLayer.fromFunction(new Driver(_, _, _, _))
  )

}
