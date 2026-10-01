package org.pi.farm.fake

import org.pi.farm.model.Message.Inbound
import org.pi.farm.runtime.StreamHub

import zio.{Chunk, Enqueue, Hub, Queue, Scope, UIO, ULayer, ZIO, ZLayer}
import zio.stream.{Take, ZStream}

case class SignalHubFake(hub: Hub[Take[Nothing, Inbound]]) extends StreamHub[Inbound] {
  def enqueue(packet: Inbound): UIO[Boolean] =
    hub.publish(Take.single(packet))

  def enqueue(packets: Chunk[Inbound]): UIO[Boolean] =
    hub.publishAll(packets.map(Take.single)).map(_.isEmpty)
}

object SignalHubFake {
  def live: ULayer[SignalHubFake] = ZLayer.scoped {
    Hub.bounded[Take[Nothing, Inbound]](1).map(SignalHubFake(_))
  }
}
