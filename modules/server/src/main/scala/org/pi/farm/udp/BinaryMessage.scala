package org.pi.farm.udp

import java.net.InetSocketAddress
import zio.Chunk

private[udp] case class BinaryMessage(ipAddress: InetSocketAddress, data: Chunk[Byte])
