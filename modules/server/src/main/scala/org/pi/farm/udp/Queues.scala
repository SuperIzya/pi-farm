package org.pi.farm.udp

import zio.{Queue, UIO, Dequeue, Enqueue}

class Queues(inboundMessages: Queue[RawMessage], outboundMessage: Queue[RawMessage]) {
  private[udp] def newIncoming(message: RawMessage): UIO[Boolean] = inboundMessages.offer(message)
  private[udp] def newOutgoing: UIO[RawMessage] = outboundMessage.take

  def inbound: Dequeue[RawMessage] = inboundMessages
  def outbound: Enqueue[RawMessage] = outboundMessage
}
