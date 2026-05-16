package org.pi.farm.fake

import org.pi.farm.model.Message.Inbound
import org.pi.farm.runtime.StreamHub

import zio.{Chunk, Enqueue, Hub, Queue, UIO, ULayer, ZIO, ZLayer}
import zio.stream.{Take, ZStream}

case class SignalHubFake(hub: Hub[Take[Nothing, Inbound]]) extends StreamHub[Inbound] {
  def enqueue(packets: Chunk[Inbound]): UIO[Boolean] =
    ZIO.foreach(packets.map(Take.single))(hub.publish).map(_.reduce(_ && _))
}

object SignalHubFake {
  def live: ULayer[SignalHubFake] = ZLayer.scoped {
    Hub.unbounded[Take[Nothing, Inbound]].map(SignalHubFake(_))
  }
}
