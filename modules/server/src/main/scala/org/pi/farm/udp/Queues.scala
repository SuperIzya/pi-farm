package org.pi.farm.udp

import zio.{Dequeue, Enqueue, Queue, UIO}
import zio.stream.ZStream

class Queues(inboundMessages: Queue[RawMessage], outboundMessage: Queue[RawMessage]) {
  private[udp] def newIncoming(message: RawMessage): UIO[Boolean]    = inboundMessages.offer(message)
  private[udp] def outgoingStream: ZStream[Any, Nothing, RawMessage] = ZStream.fromQueue(outboundMessage)

  def inbound: Dequeue[RawMessage]  = inboundMessages
  def outbound: Enqueue[RawMessage] = outboundMessage
}
