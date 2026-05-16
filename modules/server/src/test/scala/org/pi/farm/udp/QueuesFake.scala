package org.pi.farm.udp

import zio.{Dequeue, Enqueue, Queue, Scope, UIO, ULayer, ZIO, ZLayer}
import zio.stream.ZStream

case class QueuesFake(incoming: Queue[RawMessage], outgoing: Queue[RawMessage]) extends Queues {
  val inbound: Dequeue[RawMessage]  = incoming
  val outbound: Enqueue[RawMessage] = outgoing

  private[udp] def newIncoming(message: RawMessage): UIO[Boolean]    = incoming.offer(message)
  private[udp] val outgoingStream: ZStream[Any, Nothing, RawMessage] = ZStream.fromQueue(outgoing)
}

object QueuesFake {
  def live: ULayer[QueuesFake] = ZLayer.scoped {
    for {
      incoming <- Queue.bounded[RawMessage](1)
      outgoing <- Queue.bounded[RawMessage](1)
      _        <- Scope.addFinalizer {
                    incoming.shutdown *>
                      outgoing.shutdown
                  }
    } yield QueuesFake(incoming, outgoing)
  }
}
