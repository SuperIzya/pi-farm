package org.pi.farm

import org.pi.farm.model.Message.{DataPacket, Inbound, Outbound}
import org.pi.farm.storage.ControllerRepository

import zio.{Hub, Queue, RIO, Scope, ULayer, URLayer, ZIO, ZLayer}
import zio.json.ast.Json
import zio.stream.{Take, ZSink, ZStream}

package object runtime {
  type Environment =
    Scope & Controllers & ResponseHub & SignalHub & ControllerRepository & UIIncomingHub & UIIncomingQueue

  type SignalHub       = Hub[Take[Nothing, Inbound]]
  type SignalStream    = ZStream[Any, Nothing, Inbound]
  type ResponseHub     = Hub[Take[Nothing, Outbound]]
  type ResponseStream  = ZStream[Any, Nothing, Outbound]
  type ResponseQueue   = Queue[Outbound]
  type UIIncomingQueue = Queue[DataPacket]
  type UIIncomingHub   = Hub[Take[Nothing, DataPacket]]

  type Init[T] = RIO[Environment, T]

  extension [A](hub: Hub[Take[Nothing, A]]) {
    def toStream: ZStream[Any, Nothing, A] =
      ZStream
        .fromHub(hub)
        .flattenTake
  }

  object ResponseQueue {
    def live: ULayer[ResponseQueue] = ZLayer {
      Queue.sliding[Outbound](16)
    }
  }

  object ResponseStream {
    def live: URLayer[ResponseQueue, ResponseStream] = ZLayer {
      ZIO.service[ResponseQueue].map(ZStream.fromQueue(_))
    }
  }

  object ResponseHub {
    def live: URLayer[ResponseStream & Scope, ResponseHub] = ZLayer {
      ZIO.serviceWithZIO[ResponseStream](_.toHub[Nothing, Outbound](16))
    }
  }

  object UIIncomingQueue {
    def live: ULayer[UIIncomingQueue] = ZLayer {
      Queue.sliding[DataPacket](16)
    }
  }

  object UIIncomingHub {
    def live: URLayer[UIIncomingQueue & Scope, UIIncomingHub] = ZLayer {
      ZIO.serviceWithZIO[UIIncomingQueue](q => ZStream.fromQueue(q).toHub[Nothing, DataPacket](16))
    }
  }
}
