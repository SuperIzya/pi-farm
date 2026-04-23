package org.pi.farm.processing

import org.pi.farm.{OutboundStream, PiFarmSpec}
import org.pi.farm.common.plugins.CommonManifest
import org.pi.farm.fake.*
import org.pi.farm.model.{Controller, given}
import org.pi.farm.model.Message.*
import org.pi.farm.runtime.*
import org.pi.farm.storage.{ManifestRepository, ProcessingUnitsRepository}

import zio.*
import zio.internal.stacktracer.SourceLocation
import zio.stream.Take
import zio.test.{assert, check, Assertion, Gen, TestAspect, ZIOSpecDefault}

import java.net.InetSocketAddress
import scala.language.implicitConversions

object FactorySpec extends PiFarmSpec {
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
      for {
        fake <- ZIO.service[ControllerRepositoryFake]
        ctl  <- fake.create(Controller.New(1, "foo", "bar"))
        res  <- doTest(
                  Discovery(1, ctl.id, InetSocketAddress.createUnresolved("localhost", 8080)),
                  ServerDiscovered(ctl.id)
                )
      } yield res
    }
  ).provideSomeLayerShared[Scope](
    ZLayer.makeSome[Scope, ResponseHub & SignalHub & ControllerRepositoryFake](
      ConfigurationRepositoryFake.empty,
      ConfigurationStorageFake.empty,
      ResponseHub.live,
      ResponseStream.live,
      ResponseQueue.live,
      UIIncomingHub.live,
      UIIncomingQueue.live,
      Controllers.live,
      ManifestRepository.live(CommonManifest, MainManifest),
      ProcessingUnitsRepository.live,
      ControllerRepositoryFake.empty,
      Factory.live,
      ZLayer(Hub.sliding[Take[Nothing, Inbound]](16))
    )
  ) @@ TestAspect.sequential
    @@ TestAspect.samples(10)
    @@ TestAspect.shrinks(1)
}
