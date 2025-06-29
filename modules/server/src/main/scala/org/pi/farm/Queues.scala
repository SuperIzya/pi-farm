package org.pi.farm

import zio.{Dequeue, Enqueue}

case class Queues(inbound: Dequeue[RawMessage], outbound: Enqueue[RawMessage])
