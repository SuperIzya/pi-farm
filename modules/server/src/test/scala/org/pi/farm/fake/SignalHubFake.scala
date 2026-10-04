package org.pi.farm.fake

import org.pi.farm.model.Message.Inbound
import org.pi.farm.runtime.{SignalStream, StreamHub}

import zio.{Chunk, Enqueue, Hub, Queue, Scope, UIO, ULayer, URLayer, ZIO, ZLayer}
import zio.stream.{Take, ZSink, ZStream}

case class SignalHubFake(hub: Hub[Take[Nothing, Inbound]]) extends StreamHub[Inbound] {
  def enqueue(packet: Inbound): UIO[Boolean] =
    hub.publish(Take.single(packet))

  def enqueue(packets: Chunk[Inbound]): UIO[Boolean] =
    hub.publishAll(packets.map(Take.single)).map(_.isEmpty)
}

object SignalHubFake {
  def live: URLayer[SignalStream, SignalHubFake] = ZLayer.scoped {
    for {
      hub    <- Hub.sliding[Take[Nothing, Inbound]](16)
      stream <- ZIO.service[SignalStream] // Ensure the SignalStream is available in the environment
      _      <- stream.runIntoHub(hub).forkScoped
    } yield SignalHubFake(hub)
  }
}
