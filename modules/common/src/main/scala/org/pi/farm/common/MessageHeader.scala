package org.pi.farm.common

import org.pi.farm.common.Message.*

trait MessageHeader[M <: Message] {
  def header: Byte
}

object MessageHeader {
  def apply[M <: Message](using header: MessageHeader[M]): MessageHeader[M] = header

  def header[M <: Message](using header: MessageHeader[M]): Byte = header.header

  given MessageHeader[Measurement] with {
    val header: Byte = 0x00
  }

  given MessageHeader[Command] with {
    def header: Byte = 0x01
  }

  given MessageHeader[Discovery] with {
    def header: Byte = 0x02
  }

  given MessageHeader[ServerDiscovered] with {
    def header: Byte = 0x03
  }

  given MessageHeader[Ping] with {
    def header: Byte = 0x04
  }
  given MessageHeader[Pong] with {
    def header: Byte = 0x05
  }

  given MessageHeader[Error] with {
    def header: Byte = 0x06
  }
}
