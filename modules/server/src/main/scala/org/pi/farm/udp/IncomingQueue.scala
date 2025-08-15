package org.pi.farm.udp

import zio.*
import zio.stream.ZStream

trait IncomingQueue {
  def newMessage(message: IncomingMessage): Unit
  def incomingStream: ZStream[Any, Nothing, IncomingMessage]
}

object IncomingQueue {
  def live: ULayer[IncomingQueue] = ZLayer.scoped {
    for {
      queue <- Queue.bounded[IncomingMessage](2)
      runtime <- ZIO.runtime[Any]
      _ <- Scope.addFinalizer(queue.shutdown)
      stream = ZStream.fromQueue(queue)
    } yield new IncomingQueue {
      def newMessage(message: IncomingMessage): Unit = Unsafe.unsafe { unsafe ?=>
        runtime.unsafe.run(queue.offer(message))
      }

      val incomingStream: ZStream[Any, Nothing, IncomingMessage] = stream

    }
  }
}
