package org.pi.farm

import org.pi.farm.model.Message.{DataPacket, Inbound, Outbound}

import zio.Hub
import zio.json.ast.Json
import zio.stream.{ZStream, ZSink, Take}
import zio.{RIO, Scope}
import zio.Queue
import org.pi.farm.storage.ControllerRepository

package object runtime {
  type Environment =
    Scope & Controllers & ResponseHub & SignalHub & ControllerRepository

  type SignalHub      = Hub[Take[Nothing, Inbound]]
  type SignalStream   = ZStream[Any, Nothing, Inbound]
  type ResponseHub    = Hub[Take[Nothing, Outbound]]
  type ResponseStream = ZStream[Any, Nothing, Outbound]
  type ResponseQueue  = Queue[Outbound]

  type Init[T] = RIO[Environment, T]

  extension [A](hub: Hub[Take[Nothing, A]]) {
    def toStream: ZStream[Any, Nothing, A] =
      ZStream
        .fromHub(hub)
        .map(_.exit)
        .collectSuccess
        .flattenChunks
  }
}
