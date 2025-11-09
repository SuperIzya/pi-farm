package org.pi

import org.pi.farm.model.Message.{Inbound, Outbound}
import zio.{Hub, Queue, Scope, UIO, ZIO}
import zio.stream.{Take, ZStream}

package object farm {
  type SignalHub      = Hub[Take[Nothing, Inbound]]
  type SignalStream   = ZStream[Any, Nothing, Inbound]
  type ResponseHub    = Hub[Take[Nothing, Outbound]]
  type ResponseStream = ZStream[Any, Nothing, Outbound]
  type ResponseQueue  = Queue[Outbound]

  extension [A](hub: Hub[Take[Nothing, A]]) {
    def toStream: ZStream[Any, Nothing, A] =
      ZStream
        .fromHub(hub)
        .map(_.exit)
        .collectSuccess
        .flattenChunks
  }
}
