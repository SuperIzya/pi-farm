package org.pi.farm

import org.pi.farm.fake.*
import org.pi.farm.model.{Address, FlowConfiguration}
import org.pi.farm.model.Message.*
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.plugin.{DataProcessor, Inlet, Manifest, Outlet, Service}
import org.pi.farm.plugin.macros.processor
import org.pi.farm.processing.Factory
import org.pi.farm.runtime.*
import org.pi.farm.storage.{ManifestRepository, ProcessingUnitsRepository}
import org.pi.farm.udp.QueuesFake

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.stream.ZStream
import zio.test.*

import scala.collection.immutable.SortedSet
import scala.language.implicitConversions

import cats.data.NonEmptySet

object LoadTest extends PiFarmSpec {
  import Premises.*
  def spec = suite("LoadTest")(
    test("processes measurements through all configured processors") {

      val genData = for {
        intValue    <- Random.nextIntBetween(1, 100000)
        stringValue <- Random.nextPrintableChar.replicateZIO(10)
      } yield (intValue, new String(stringValue.toArray))

      ZStream
        .repeatZIO(genData)
        .take(1000000)
        .mapZIOPar(32) {
          case (intValue, stringValue) =>
            ZIO.scoped {
              for {
                signalHub    <- ZIO.service[SignalHubFake]
                response     <- ZIO.service[ResponseHub]
                subscription <- response.subscribe
                data          = Measurements(
                                  controllerId,
                                  Map(
                                    peripheryOutput -> Map(
                                      intChannel    -> valueJson(intValue),
                                      stringChannel -> valueJson(stringValue)
                                    )
                                  )
                                )
                expected      =
                  Set(
                    Command(
                      controllerId,
                      Map(peripheryOutput -> Map(intChannel -> valueJson(intValue * 10)))
                    ),
                    Command(
                      controllerId,
                      Map(peripheryOutput -> Map(stringChannel -> valueJson(stringValue.reverse)))
                    ),
                    Command(
                      controllerId,
                      Map(
                        peripheryOutput -> Map(
                          intChannel    -> valueJson(intValue * 2),
                          stringChannel -> valueJson(stringValue + stringValue)
                        )
                      )
                    )
                  )

                _ <- signalHub.enqueue(data)

                _ <- subscription
                       .collect {
                         case c: Command if expected.contains(c) => c
                       }
                       .scan(Set.empty[Command])(_ + _)
                       .takeUntil(_ == expected)
                       .runCollect
              } yield ()
            }

        }
        .runDrain
        .as(assertCompletes)

    } @@ timeout @@ TestAspect.ignore
  ).provideSomeLayerShared[Scope](layers)

  val timeout = TestAspect.timeout(15.minutes)

  override def aspects = Chunk(
    timeout,
    TestAspect.parallel,
    TestAspect.withLiveEnvironment,
    TestAspect.timed
  )

  private val countResponses = ZLayer {
    for {
      scope        <- ZIO.scope
      responseHub  <- ZIO.service[ResponseHub]
      signalHub    <- ZIO.service[SignalHubFake]
      input        <- signalHub.subscribe
      subscription <- responseHub.subscribe
      total        <- Ref.make(0L)
      _            <- subscription
                        .zipWithIndex
                        .map { case (_, index) => index }
                        .mapZIO(_ => total.updateAndGet(_ + 1))
                        .runDrain
                        .forkScoped
      _            <- input
                        .zipWithIndex
                        .map { case (_, index) => index }
                        /* .tap { count =>
                          ZIO.succeed(count).debug(s"Ingested input so far")
                        } */
                        .runDrain
                        .forkScoped
      _            <- scope.addFinalizer(
                        total.get.flatMap(count => ZIO.logInfo(s"Total responses counted: $count"))
                      )
    } yield ()
  }

  private val layers = ZLayer.makeSome[Scope, SignalHubFake & ResponseHub](
    countResponses,
    ConfigurationRepositoryFake.empty,
    FlowConfigurationChangesFake.generated(Set(configuration)),
    ResponseHub.live,
    ResponseQueue.live,
    UIIncomingHub.live,
    UIIncomingQueue.live,
    Controllers.live,
    ManifestRepository.live(manifest),
    ProcessingUnitsRepository.live,
    ControllerRepositoryFake.empty,
    Factory.live,
    SignalStream.live,
    QueuesFake.live,
    SignalHubFake.live
  )

  object Premises {
    @processor(name = "MultiplyInt", description = "Multiplies an integer by ten")
    object MultiplyInt extends DataProcessor {
      type ParamsType = Unit
      given paramsCodec: JsonCodec[ParamsType] = DataProcessor.noParamsCodec

      val input  = Inlet[Int]("input", "Integer input", "")
      val output = Outlet[Int]("output", "Integer output", "")

      def process(value: Int): Int = value * 10

      def work = from(input).to(output).via(process)
    }

    @processor(name = "ReverseString", description = "Reverses a string")
    object ReverseString extends DataProcessor {
      type ParamsType = Unit
      given paramsCodec: JsonCodec[ParamsType] = DataProcessor.noParamsCodec

      val input  = Inlet[String]("input", "String input", "")
      val output = Outlet[String]("output", "String output", "")

      def process(value: String): String = value.reverse

      def work = from(input).to(output).via(process)
    }

    @processor(name = "DoubleValues", description = "Doubles an integer and a string")
    object DoubleValues extends DataProcessor {
      type ParamsType = Unit
      given paramsCodec: JsonCodec[ParamsType] = DataProcessor.noParamsCodec

      val intInput     = Inlet[Int]("intInput", "Integer input", "")
      val stringInput  = Inlet[String]("stringInput", "String input", "")
      val intOutput    = Outlet[Int]("intOutput", "Integer output", "")
      val stringOutput = Outlet[String]("stringOutput", "String output", "")

      def process(intValue: Int, stringValue: String): (Int, String) =
        (intValue * 2, stringValue * 2)

      def work = from(intInput, stringInput).to(intOutput, stringOutput).via(process)
    }

    val manifest: Manifest = new Manifest {
      val version: String                  = "test"
      val name: String                     = "Load test processors"
      val processors: Chunk[DataProcessor] = Chunk(MultiplyInt, ReverseString, DoubleValues)
      val services: Chunk[Service.Creator] = Chunk.empty
    }

    val controllerId    = 1
    val peripheryOutput = "output".toPeripheryName
    val intChannel      = "int".toPeripheryChannelName
    val stringChannel   = "string".toPeripheryChannelName

    def valueJson[T: JsonEncoder](value: T): Json = Data(value).toJsonAST.toOption.get

    private def processor(
      unit: String,
      inbound: Chunk[Address],
      outbound: Chunk[Address],
      graphId: String
    ): FlowConfiguration.Processor =
      FlowConfiguration.Processor(unit, Json.Obj(), inbound, outbound, graphId)

    private def address(channelName: PeripheryChannelName, outlet: String) =
      Address(controllerId, peripheryOutput, channelName, outlet)

    val configuration = FlowConfiguration(
      id = 1,
      name = "load-test",
      graphData = Json.Obj(),
      description = "Three processors sharing one controller",
      processors = NonEmptySet.fromSetUnsafe(
        SortedSet(
          processor(
            unit = "MultiplyInt",
            inbound = Chunk(address(intChannel, "input")),
            outbound = Chunk(address(intChannel, "output")),
            graphId = "multiply-int"
          ),
          processor(
            unit = "ReverseString",
            inbound = Chunk(address(stringChannel, "input")),
            outbound = Chunk(address(stringChannel, "output")),
            graphId = "reverse-string"
          ),
          processor(
            unit = "DoubleValues",
            inbound = Chunk(
              address(intChannel, "intInput"),
              address(stringChannel, "stringInput")
            ),
            outbound = Chunk(
              address(intChannel, "intOutput"),
              address(stringChannel, "stringOutput")
            ),
            graphId = "double-values"
          )
        )
      ),
      previewSvg = None
    )
  }

}
