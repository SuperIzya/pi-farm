package org.pi.farm.udp

import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import zio.{URLayer, ZLayer}

class UdpChannelHandler(messageHandler: BinaryMessage => Unit) extends SimpleChannelInboundHandler[BinaryMessage] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: BinaryMessage): Unit =
    messageHandler(msg)

  protected def initChannel(ch: NioDatagramChannel): Unit = {
    val pipeline = ch.pipeline()
    pipeline.addLast("decoder", Decoder)
    println
    ()
  }
}

object UdpChannelHandler {
  def live: URLayer[IncomingQueue, UdpChannelHandler] =
    ZLayer.fromFunction((q: IncomingQueue) => new UdpChannelHandler(q.newMessage))
}
