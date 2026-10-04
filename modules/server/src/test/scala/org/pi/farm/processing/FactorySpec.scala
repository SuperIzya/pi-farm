package org.pi.farm.processing

import org.pi.farm.{OutboundRawStream, PiFarmSpec}
import org.pi.farm.common.plugins.CommonManifest
import org.pi.farm.fake.*
import org.pi.farm.model.{Address, Controller, FlowConfiguration, given}
import org.pi.farm.model.Message.*
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.plugin.{DataProcessor, Inlet, Manifest, Outlet, Service}
import org.pi.farm.plugin.macros.processor
import org.pi.farm.runtime.*
import org.pi.farm.storage.{ManifestRepository, ProcessingUnitsRepository}
import org.pi.farm.udp.{Queues, QueuesFake, RawMessage}

import zio.*
import zio.internal.stacktracer.SourceLocation
import zio.json.*
import zio.json.ast.Json
import zio.stream.{Take, ZStream}
import zio.test.{assertCompletes, assertTrue, check, checkN, Gen, Live, Spec, TestAspect}

import java.net.InetSocketAddress
import scala.language.implicitConversions

import cats.data.NonEmptySet

object FactorySpec extends PiFarmSpec {

  private val address = InetSocketAddress.createUnresolved("localhost/127.0.0.1", 1234)

  @processor(name = "Factory processor 1", description = "Integer parameter lifecycle test")
  object IntProcessor extends DataProcessor {
    case class Params(value: Int)
    type ParamsType = Params

    given paramsCodec: JsonCodec[ParamsType] = JsonCodec[Int].transform(Params(_), _.value)

    final val input  = Inlet[Int]("input", "units")
    final val output = Outlet[String]("output", "units")

    def process(value: Int)(using params: ParamsType): String = s"processor 1 ${params.value}"

    def work = from(input).to(output).via(process)
  }

  @processor(name = "Factory processor 2", description = "String parameter lifecycle test")
  object StringProcessor extends DataProcessor {
    case class Params(value: String)
    type ParamsType = Params
    given paramsCodec: JsonCodec[ParamsType] = JsonCodec[String].transform(Params(_), _.value)

    final val input  = Inlet[Int]("input", "units")
    final val output = Outlet[String]("output", "units")

    def process(value: Int)(using params: ParamsType): String = s"processor 2 ${params.value}"

    def work = from(input).to(output).via(process)
  }

  private val lifecycleManifest: Manifest = new Manifest {
    val version: String                  = "test"
    val name: String                     = "Factory lifecycle test"
    val processors: Chunk[DataProcessor] = Chunk(IntProcessor, StringProcessor)
    val services: Chunk[Service.Creator] = Chunk.empty
  }

  private def configuration(unit: String, parameters: Json): FlowConfiguration.New =
    FlowConfiguration.New(
      name = unit.toName,
      description = "Factory lifecycle test",
      graphData = Json.Obj(),
      processors = NonEmptySet.one(
        FlowConfiguration.Processor(
          unit = unit,
          parameters = parameters,
          inbound = Chunk(Address(1, "sensor", "out", "input")),
          outbound = Chunk(Address(2, "actuator", "in", "output")),
          graphId = unit
        )
      ),
      previewSvg = None
    )

  private def doTest(in: Inbound, out: Outbound) = ZIO.scoped {
    for {
      signal       <- ZIO.service[SignalHubFake]
      outbound     <- ZIO.service[ResponseHub]
      subscription <- outbound.subscribe
      _            <- signal
                        .enqueue(in)
      _            <- Live.live(subscription.interruptAfter(300.millis).filter(_ == out).take(1).runDrain)
    } yield ()
  }

  def spec = suite("FactorySpec")(
    test("Should load PingPong service") {
      doTest(Ping(42), Pong(42)).as(assertCompletes)
    },
    test("Should load Discovery service") {
      for {
        fake        <- ZIO.service[ControllerRepositoryFake]
        ctl         <- fake.create(Controller.New(1, "foo", "bar", None))
        _           <- doTest(
                         Discovery(ctl.id, address),
                         ServerDiscovered(ctl.id)
                       )
        controllers <- ZIO.service[Controllers]
        byAddress   <- controllers.getController(address)
        byId        <- controllers.getAddress(ctl.id)
      } yield assertTrue(
        byAddress.contains(ctl),
        byId.exists(p => p.toString() == address.toString())
      )
    },
    test("Should reload changed configurations and stop deleted configurations") {

      def expectedOutput(value: String): Outbound =
        Command(
          2,
          Map("actuator".toPeripheryName -> Map("in".toPeripheryChannelName -> Data(value).toJsonAST.toOption.get))
        )

      val lifecycleOutputs = ZIO.scoped {
        for {

          response     <- ZIO.service[ResponseHub]
          subscription <- response.subscribe
          _            <- Live.live(ZIO.sleep(300.millis))
          signal       <- ZIO.service[SignalHubFake]
          _            <- signal.enqueue(FlatDataPacket(1, "sensor", "out", Data(42).toJsonAST.toOption.get))
          outputs      <-
            Live.live(
              subscription
                .interruptAfter(300.millis)
                .collect {
                  case c: Command => c
                }
                .runCollect
            )
        } yield outputs
      }

      ZIO.scoped {
        for {
          changes <- ZIO.service[FlowConfigurationChangesFake]
          first   <- changes.create(configuration("Factory processor 1", Json.Num(10)))
          second  <- changes.create(configuration("Factory processor 2", Json.Str("initial")))
          added   <- lifecycleOutputs
          updated  = first.copy(processors = NonEmptySet.one(first.processors.head.copy(parameters = Json.Num(20))))
          result  <- changes.update(first.id, updated)
          changed <- lifecycleOutputs
          _       <- changes.delete(second.id)
          deleted <- lifecycleOutputs
        } yield assertTrue(
          added.size == 2,
          added.toSet == Set(expectedOutput("processor 1 10"), expectedOutput("processor 2 initial")),
          result.contains(updated),
          changed.size == 2,
          changed.toSet == Set(expectedOutput("processor 1 20"), expectedOutput("processor 2 initial")),
          deleted == Chunk(expectedOutput("processor 1 20"))
        )
      }
    }
  ).provideSomeLayerShared[Scope](layer) @@ TestAspect.debug

  def layer =
    ZLayer
      .makeSome[
        Scope,
        FlowConfigurationChangesFake & ResponseHub & SignalHubFake & ControllerRepositoryFake & QueuesFake &
          Controllers & Scope
      ](
        ConfigurationRepositoryFake.empty,
        FlowConfigurationChangesFake.empty,
        ResponseQueue.live,
        ResponseHub.live,
        UIIncomingHub.live,
        UIIncomingQueue.live,
        Controllers.live,
        ManifestRepository.live(CommonManifest, MainManifest, lifecycleManifest),
        ProcessingUnitsRepository.live,
        ControllerRepositoryFake.empty,
        Factory.live,
        SignalStream.live,
        QueuesFake.live,
        SignalHubFake.live
      )
}
