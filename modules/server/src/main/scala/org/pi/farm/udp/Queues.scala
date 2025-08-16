package org.pi.farm.udp

import zio.stream.ZStream
import zio.{Dequeue, Enqueue, Queue, UIO}

class Queues(inboundMessages: Queue[RawMessage], outboundMessage: Queue[RawMessage]) {
  private[udp] def newIncoming(message: RawMessage): UIO[Boolean] = inboundMessages.offer(message)
  private[udp] def outgoingStream: ZStream[Any, Nothing, RawMessage] = ZStream.fromQueue(outboundMessage)

  def inbound: Dequeue[RawMessage] = inboundMessages
  def outbound: Enqueue[RawMessage] = outboundMessage
}
