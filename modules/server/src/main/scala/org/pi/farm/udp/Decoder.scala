package org.pi.farm.udp

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramPacket
import io.netty.handler.codec.MessageToMessageDecoder
import zio.Chunk

import java.util

object Decoder extends MessageToMessageDecoder[DatagramPacket] {

  protected def decode(ctx: ChannelHandlerContext, msg: DatagramPacket, out: util.List[AnyRef]): Unit =
    out.add(BinaryMessage(msg.sender(), Chunk.fromByteBuffer(msg.content().nioBuffer())))

}
