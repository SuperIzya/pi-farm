package org.pi.farm.fake

import org.pi.farm.PiFarmSpec
import org.pi.farm.runtime.SignalStream

import zio.*
import zio.stream.ZStream
import zio.test.*

object SignalHubFakeSpec extends PiFarmSpec {
  def spec = suite("SignalHubFake")(
    test("waits for all subscribers and removes them when their scope closes") {
      ZIO.scoped {
        for {
          signal    <- ZIO.service[SignalHubFake]
          ready     <- signal.awaitSubscribers(2).forkScoped
          before    <- ready.poll
          result    <- ZIO.scoped {
                         for {
                           _        <- signal.subscribe
                           _        <- signal.awaitSubscribers(1)
                           afterOne <- ready.poll
                           _        <- signal.subscribe
                           _        <- ready.join
                         } yield assertTrue(before.isEmpty, afterOne.isEmpty)
                       }
          remaining <- signal.subscribers.get.commit
        } yield result && assertTrue(remaining == 0)
      }
    }
  ).provideLayer(ZLayer.succeed[SignalStream](ZStream.never) >>> SignalHubFake.live)
}
