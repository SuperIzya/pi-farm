package org.pi.farm.udp

import java.net.InetSocketAddress
import zio.Chunk

case class IncomingMessage (sender: InetSocketAddress, data: Chunk[Byte])
