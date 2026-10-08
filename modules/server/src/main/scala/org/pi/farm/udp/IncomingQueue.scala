package org.pi.farm.udp

import zio.*
import zio.stream.ZStream

trait IncomingQueue {
  def newMessage(message: BinaryMessage): Unit
  def incomingStream: ZStream[Any, Nothing, BinaryMessage]
}

object IncomingQueue {
  def live: URLayer[Scope, IncomingQueue] = ZLayer {
    for {
      queue   <- Queue.sliding[BinaryMessage](2)
      runtime <- ZIO.runtime[Any]
      _       <- Scope.addFinalizer(queue.shutdown)
      stream   = ZStream.fromQueue(queue)
    } yield new IncomingQueue {
      def newMessage(message: BinaryMessage): Unit = Unsafe.unsafe { unsafe ?=>
        runtime.unsafe.run(queue.offer(message))
      }

      val incomingStream: ZStream[Any, Nothing, BinaryMessage] = stream

    }
  }
}
