package org.pi.farm.udp

import zio.Chunk

import java.net.InetSocketAddress

private[udp] case class BinaryMessage(ipAddress: InetSocketAddress, data: Chunk[Byte])
