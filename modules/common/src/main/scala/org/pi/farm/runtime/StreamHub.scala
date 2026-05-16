package org.pi.farm.runtime

import org.pi.farm.model.Message.{Inbound, Outbound}

import zio.{Hub, Scope, URIO, URLayer, ZIO, ZLayer}
import zio.stream.{Take, ZStream}

trait StreamHub[T] {
  protected def hub: Hub[Take[Nothing, T]]
  def subscribe: URIO[Scope, ZStream[Any, Nothing, T]] = ZStream.fromHubScoped(hub).map(_.flattenTake)
}

object StreamHub {

  final case class SignalHub(hub: Hub[Take[Nothing, Inbound]]) extends StreamHub[Inbound]

  object SignalHub {
    def live: URLayer[SignalStream & Scope, SignalHub] = ZLayer {
      ZIO.serviceWithZIO[SignalStream] { stream =>
        stream.toHub(8).map(SignalHub(_))
      }
    }
  }

  final case class ResponseHub(hub: Hub[Take[Nothing, Outbound]]) extends StreamHub[Outbound]

  object ResponseHub {
    def live: URLayer[ResponseStream & Scope, ResponseHub] = ZLayer {
      ZIO.serviceWithZIO[ResponseStream] { stream =>
        stream.toHub(8).map(ResponseHub(_))
      }
    }
  }
}
