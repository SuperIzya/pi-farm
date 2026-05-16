package org.pi.farm.udp

import zio.{Dequeue, Enqueue, Queue, UIO, ULayer, ZLayer}
import zio.stream.ZStream

trait Queues {
  def inbound: Dequeue[RawMessage]
  def outbound: Enqueue[RawMessage]

  private[udp] def newIncoming(message: RawMessage): UIO[Boolean]
  private[udp] def outgoingStream: ZStream[Any, Nothing, RawMessage]
}

object Queues {
  def make(size: Int): UIO[Queues] =
    for {
      inboundMessages  <- Queue.sliding[RawMessage](size)
      outboundMessages <- Queue.sliding[RawMessage](size)
    } yield new Queues {
      private[udp] def newIncoming(message: RawMessage): UIO[Boolean]    = inboundMessages.offer(message)
      private[udp] val outgoingStream: ZStream[Any, Nothing, RawMessage] = ZStream.fromQueue(outboundMessages)

      val inbound: Dequeue[RawMessage]  = inboundMessages
      val outbound: Enqueue[RawMessage] = outboundMessages
    }
}
