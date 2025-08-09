package org.pi.farm.processing

import org.pi.farm.model.Controller
import org.pi.farm.model.Message.*
import org.pi.farm.fake.*
import org.pi.farm.{Controllers, ResponseHub, SignalHub}
import zio.internal.stacktracer.SourceLocation
import zio.stream.Take
import zio.test.{Gen, TestAspect, ZIOSpecDefault, assert, check, Assertion}
import zio.*

import java.net.InetSocketAddress

object FactorySpec extends ZIOSpecDefault {
  private def doTest(in: Inbound, out: Outbound)(using Trace, SourceLocation) = {
    for {
      inbound  <- ZIO.service[SignalHub]
      response <- ZIO.service[ResponseHub]
      outbound <- response.subscribe
      _        <- inbound.offer(Take.single(in))
      res      <- outbound.take.map(_.exit).exit.flatMap(_.flatten).head
    } yield assert(res)(Assertion.equalTo(out))
  }

  def spec = suite("FactorySpec")(
    test("Should load PingPong processing unit") {
      check(Gen.int(100, 200)) { controllerId =>
        doTest(Ping(controllerId), Pong(controllerId))
      }
    },
    test("Should load Discovery processing unit") {
      check(Gen.int(100, 200)) { controllerId =>
        for {
          fake <- ZIO.service[ControllerRepositoryFake]
          _    <- fake.create(Controller(controllerId, 1))
          res  <- doTest(
            Discovery(1, controllerId, InetSocketAddress.createUnresolved("localhost", 8080)),
            ServerDiscovered(controllerId)
          )
        } yield res
      }
    }
  ).provideSomeLayerShared[Scope](
    ZLayer.makeSome[Scope, ResponseHub & SignalHub & ControllerRepositoryFake](
      ConfigurationRepositoryFake.empty,
      ConfigurationStorageFake.empty,
      ProcessingManager.live,
      ControllerRepositoryFake.empty,
      Controllers.live,
      Factory.live,
      ZLayer(Hub.sliding[Take[Nothing, Inbound]](16))
    )
  ) @@ TestAspect.timeout(20.seconds)
    @@ TestAspect.sequential
    @@ TestAspect.samples(10)
    @@ TestAspect.shrinks(1)
}
