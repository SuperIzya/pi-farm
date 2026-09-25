package org.pi.farm.udp

import zio.*
import zio.http.netty.{ChannelType, NettyConfig, NettyFutureExecutor}
import zio.http.netty.server.ServerEventLoopGroups

import io.netty.bootstrap.Bootstrap
import io.netty.channel.{Channel, ChannelInitializer, ChannelOption}
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.util.ResourceLeakDetector

class Driver(
  config: UdpConfig,
  nettyConfig: NettyConfig,
  channelHandler: UdpChannelHandler,
  eventLoopGroups: ServerEventLoopGroups
) {
  def start: RIO[Scope, Channel] = {
    for {
      chf     <- ZIO.attempt {
                   new Bootstrap()
                     .group(eventLoopGroups.boss)
                     .option(ChannelOption.AUTO_CLOSE, true)
                     .option(ChannelOption.SO_BROADCAST, true)
                     .channel(classOf[NioDatagramChannel])
                     .handler(new ChannelInitializer[NioDatagramChannel] {
                       override def initChannel(ch: NioDatagramChannel): Unit =
                         ch.pipeline().addLast(channelHandler)
                     })
                     .bind(config.port)
                     .sync()
                 }
      _       <- NettyFutureExecutor.scoped(chf)
      _       <- ZIO.dieMessage(s"Failed to bind to port ${config.port}").unless(chf.isSuccess)
      _       <- ZIO.succeed(ResourceLeakDetector.setLevel(nettyConfig.leakDetectionLevel.toNetty))
      channel <- ZIO.attempt(chf.channel())
      _       <- Scope.addFinalizer(NettyFutureExecutor.executed(channel.close()).ignoreLogged)
    } yield channel
  }
}

object Driver {

  private val nettyConfig = NettyConfig
    .default
    .channelType(ChannelType.NIO)
    .maxThreads(32)
    .bossGroup(NettyConfig.default.bossGroup.copy(channelType = ChannelType.NIO, nThreads = 32))

  def live: URLayer[UdpConfig, Driver & IncomingQueue] = ZLayer.makeSome[UdpConfig, Driver & IncomingQueue](
    ZLayer.succeed(nettyConfig),
    ServerEventLoopGroups.live,
    IncomingQueue.live,
    UdpChannelHandler.live,
    ZLayer.fromFunction(new Driver(_, _, _, _))
  )

}
