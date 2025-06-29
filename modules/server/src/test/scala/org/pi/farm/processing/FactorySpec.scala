package org.pi.farm.processing

import org.pi.farm.{Controllers, ResponseHub, SignalHub}
import org.pi.farm.common.Message.*
import zio.internal.stacktracer.SourceLocation
import zio.stream.Take
import zio.{Duration, Hub, Scope, Trace, ZIO, ZLayer}
import zio.test.{Gen, TestAspect, ZIOSpecDefault, assertCompletes, assertTrue, checkAll}

import java.net.InetSocketAddress

object FactorySpec extends ZIOSpecDefault {
  private def doTest(in: Inbound, out: Outbound)(using Trace, SourceLocation)  = {
    for {
      inbound <- ZIO.service[SignalHub]
      response <- ZIO.service[ResponseHub]
      outbound <- response.subscribe
      _ <- inbound.offer(Take.single(in))
      res <- outbound.take.map(_.exit).exit.flatMap(_.flatten).head
    } yield assertTrue(res == out)
  }

  def spec = suite("FactorySpec")(
    test("Should load PingPong processing unit") {
      checkAll(Gen.int) { controllerId =>
        doTest(Ping(controllerId), Pong(controllerId))
      }
    },
    test("Should load Discovery processing unit") {
      checkAll(Gen.int) { controllerId =>
        doTest(Discovery("test", 1, InetSocketAddress.createUnresolved("localhost", 8080)), ServerDiscovered(1))
      }
    }
  ).provideSomeLayer[Scope](
    ZLayer.makeSome[Scope, ResponseHub & SignalHub](
      ConfigurationStorage.live,
      ProcessingStorage.live,
      Controllers.live,
      Factory.live,
      ZLayer(Hub.sliding[Take[Nothing, Inbound]](16))
    )
  ) @@ TestAspect.timeout(Duration.fromSeconds(2))
}
