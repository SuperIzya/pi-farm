package org.pi.farm

import org.pi.farm.model.Message.{DataPacket, Inbound, Outbound}
import org.pi.farm.storage.ControllerRepository

import zio.*
import zio.json.ast.Json
import zio.stream.{Take, ZSink, ZStream}

package object runtime {
  type Environment =
    Scope & Controllers & SignalHub & ControllerRepository & UIIncomingHub & UIIncomingQueue & ResponseQueue

  val SignalHub   = StreamHub.SignalHub
  val ResponseHub = StreamHub.ResponseHub

  type SignalHub       = StreamHub[Inbound]
  type ResponseHub     = StreamHub[Outbound]
  type SignalStream    = ZStream[Any, Nothing, Inbound]
  type ResponseStream  = ZStream[Any, Nothing, Outbound]
  type ResponseQueue   = Queue[Outbound]
  type UIIncomingQueue = Queue[DataPacket]
  type UIIncomingHub   = Hub[Take[Nothing, DataPacket]]

  type Init[T] = RIO[Environment, T]

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
