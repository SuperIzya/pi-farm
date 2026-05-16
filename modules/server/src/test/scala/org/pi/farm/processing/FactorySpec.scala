package org.pi.farm.processing

import org.pi.farm.{OutboundStream, PiFarmSpec}
import org.pi.farm.common.plugins.CommonManifest
import org.pi.farm.fake.*
import org.pi.farm.model.{Controller, given}
import org.pi.farm.model.Message.*
import org.pi.farm.runtime.*
import org.pi.farm.storage.{ManifestRepository, ProcessingUnitsRepository}
import org.pi.farm.udp.{Queues, QueuesFake, RawMessage}

import zio.*
import zio.internal.stacktracer.SourceLocation
import zio.json.*
import zio.stream.{Take, ZStream}
import zio.test.{assertTrue, checkN, Gen, Live, TestAspect}

import java.net.InetSocketAddress
import scala.language.implicitConversions

object FactorySpec extends PiFarmSpec {

  private val address = InetSocketAddress.createUnresolved("localhost/127.0.0.1", 1234)

  private def doTest(in: Inbound, out: Outbound)(using Trace, SourceLocation) = {
    for {
      queues       <- ZIO.service[QueuesFake]
      outbound     <- ZIO.service[ResponseHub]
      subscription <- outbound.subscribe
      _            <- queues
                        .incoming
                        .offer(RawMessage(address, in.toJson))
      res          <- subscription.tap(i => ZIO.logInfo(s"Received output: $i")).take(1).runHead
    } yield assertTrue(res.contains(out))
  }

  def spec = suite("FactorySpec")(
    test("Should load PingPong service") {
      checkN(5)(Gen.int(100, 200)) { controllerId =>
        ZIO.serviceWithZIO[Controllers](_.addController(address, Controller(controllerId, 20, "bar", ""))) *>
          doTest(Ping(controllerId), Pong(controllerId))
      }
    }.provideSomeLayerShared[Scope](layer),
    test("Should load Discovery service") {
      for {
        fake        <- ZIO.service[ControllerRepositoryFake]
        ctl         <- fake.create(Controller.New(1, "foo", "bar"))
        res         <- doTest(
                         Discovery(1, ctl.id, address),
                         ServerDiscovered(ctl.id)
                       )
        controllers <- ZIO.service[Controllers]
        byAddress   <- controllers.getController(address)
        byId        <- controllers.getAddress(ctl.id)
      } yield res && assertTrue(
        byAddress.contains(ctl) &&
          byId.exists(p => p.toString() == address.toString())
      )
    }.provideSomeLayer[Scope](layer)
  )

  def layer =
    ZLayer
      .makeSome[
        Scope,
        ResponseHub & ControllerRepositoryFake & QueuesFake & Controllers & Scope
      ](
        ConfigurationRepositoryFake.empty,
        ConfigurationStorageFake.empty,
        ResponseStream.live,
        ResponseQueue.live,
        ResponseHub.live,
        UIIncomingHub.live,
        UIIncomingQueue.live,
        Controllers.live,
        ManifestRepository.live(CommonManifest, MainManifest),
        ProcessingUnitsRepository.live,
        ControllerRepositoryFake.empty,
        Factory.live,
        QueuesFake.live,
        SignalStream.live,
        SignalHub.live
      )
}
