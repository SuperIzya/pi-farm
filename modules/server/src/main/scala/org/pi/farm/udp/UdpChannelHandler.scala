package org.pi.farm.udp

import zio.{Chunk, URLayer, ZLayer}

import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.util.CharsetUtil

class UdpChannelHandler(messageHandler: BinaryMessage => Unit) extends SimpleChannelInboundHandler[DatagramPacket] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: DatagramPacket): Unit = {
    val content = msg.content()
    val copy    = new Array[Byte](content.readableBytes())
    content.getBytes(content.readerIndex(), copy)
    val payload = Chunk.fromByteBuffer(msg.content().nioBuffer())
    val message = BinaryMessage(msg.sender(), payload)
    messageHandler(message)
  }
}

object UdpChannelHandler {
  def live: URLayer[IncomingQueue, UdpChannelHandler] =
    ZLayer.fromFunction((q: IncomingQueue) => new UdpChannelHandler(q.newMessage))
}
