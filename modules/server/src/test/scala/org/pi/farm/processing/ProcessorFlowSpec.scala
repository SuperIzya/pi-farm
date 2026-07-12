package org.pi.farm.processing

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.*
import org.pi.farm.model.{Address, FlowConfiguration}
import org.pi.farm.model.Message.*
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.plugin.{DataProcessor, Inlet, Manifest, Outlet, Service}
import org.pi.farm.plugin.macros.processor
import org.pi.farm.runtime.*
import org.pi.farm.storage.{ManifestRepository, ProcessingUnitsRepository}
import org.pi.farm.udp.{QueuesFake, RawMessage}

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.stream.{Take, ZStream}
import zio.test.*
import zio.test.Assertion.*

import java.net.InetSocketAddress
import scala.collection.immutable.SortedSet
import scala.language.implicitConversions

import cats.data.NonEmptySet

object ProcessorFlowSpec extends PiFarmSpec {

  // --- Test Processors ---

  /** Averages two Double inputs and produces one Double output. */
  @processor(name = "Averager", description = "Averages two input values")
  object AveragerProcessor extends DataProcessor {
    type ParamsType = Unit
    given paramsCodec: JsonCodec[ParamsType] = DataProcessor.noParamsCodec

    final val inputA = Inlet[Double]("inputA", "First value", "units")
    final val inputB = Inlet[Double]("inputB", "Second value", "units")

    final val output = Outlet[Double]("output", "Averaged value", "units")

    def process(a: Double, b: Double): Double = (a + b) / 2.0

    def work = from(inputA, inputB).to(output).via(process)
  }

  /** Takes one Double input and produces two outputs: doubled and halved. */
  @processor(name = "SplitTransform", description = "Produces doubled and halved outputs")
  object SplitTransformProcessor extends DataProcessor {
    type ParamsType = Unit
    given paramsCodec: JsonCodec[ParamsType] = DataProcessor.noParamsCodec

    final val input = Inlet[Double]("input", "Input value", "units")

    final val doubled = Outlet[Double]("doubled", "Doubled value", "units")
    final val halved  = Outlet[Double]("halved", "Halved value", "units")

    def process(v: Double): (Double, Double) = (v * 2.0, v / 2.0)

    def work = from(input).to(doubled, halved).via(process)
  }

  /** Takes two Double inputs and produces two outputs: sum and difference, scaled by a parameter. */
  @processor(name = "SumDiff", description = "Produces sum and difference of two inputs")
  object SumDiffProcessor extends DataProcessor {
    case class Params(scale: Double)
    type ParamsType = Params
    given paramsCodec: JsonCodec[ParamsType] = DeriveJsonCodec.gen[Params]

    final val inputX = Inlet[Double]("inputX", "First operand", "units")
    final val inputY = Inlet[Double]("inputY", "Second operand", "units")

    final val sum  = Outlet[Double]("sum", "Scaled sum", "units")
    final val diff = Outlet[Double]("diff", "Scaled difference", "units")

    def process(x: Double, y: Double)(using p: Params): (Double, Double) =
      ((x + y) * p.scale, (x - y) * p.scale)

    def work = from(inputX, inputY).to(sum, diff).via(process)
  }

  private val testManifest: Manifest = new Manifest {
    val version: String                  = "test"
    val name: String                     = "Test Processors"
    val processors: Chunk[DataProcessor] = Chunk(AveragerProcessor, SplitTransformProcessor, SumDiffProcessor)
    val services: Chunk[Service.Creator] = Chunk.empty
  }

  // --- Helpers ---

  private def dataJson(value: Double): Json = Data(value).toJsonAST.toOption.get

  private def sendAndCollect(packets: Chunk[Inbound], expectedCount: Int) =
    for {
      signalHub    <- ZIO.service[SignalHubFake]
      response     <- ZIO.service[ResponseHub]
      subscription <- response.subscribe
      _            <- signalHub.enqueue(packets)
      results      <- subscription.take(expectedCount).runCollect
    } yield extractDataPoints(results)

  private def extractDataPoints(outbound: Chunk[Outbound]): Chunk[PackedDataPacket] =
    outbound.collect {
      case Command(_, dataPoints) => dataPoints
    }

  private def findDp(
    dps: Chunk[PackedDataPacket],
    controllerId: ControllerId,
    peripheryName: PeripheryName
  ): Option[FlatDataPacket] =
    for {
      dp   <- dps.find(_.controllerId == controllerId)
      map  <- dp.rest.get(peripheryName)
      pair <- map.headOption
    } yield FlatDataPacket(dp.controllerId, peripheryName, pair._1, pair._2)

  private def flowConfig(id: Int, name: String, processors: FlowConfiguration.Processor*): FlowConfiguration =
    FlowConfiguration(
      id = id,
      name = name,
      description = s"Test config: $name",
      processors = NonEmptySet.fromSetUnsafe(SortedSet.from(processors)),
      graphData = Json.Obj()
    )

  private def layers(configs: Set[FlowConfiguration]) =
    ZLayer.makeSome[Scope, QueuesFake & SignalHubFake & ConfigurationStorageFake & ResponseHub](
      ConfigurationRepositoryFake.empty,
      ConfigurationStorageFake.generated(configs),
      QueuesFake.live,
      ResponseStream.live,
      ResponseHub.live,
      ResponseQueue.live,
      UIIncomingHub.live,
      UIIncomingQueue.live,
      Controllers.live,
      ManifestRepository.live(testManifest),
      ProcessingUnitsRepository.live,
      ControllerRepositoryFake.empty,
      Factory.live,
      SignalHubFake.live,
      ZLayer {
        Live.live(ZIO.sleep(300.millis))
      }
    )

  // --- Test Suite ---

  def spec = suite("ProcessorFlowSpec")(
    suite("Single processor")(
      test("Averager: two inlets, one outlet") {
        // Averager: (3.0 + 7.0) / 2 = 5.0
        sendAndCollect(
          Chunk(
            FlatDataPacket(1, "sensorA", "outA", dataJson(3.0)),
            FlatDataPacket(2, "sensorB", "outB", dataJson(7.0))
          ),
          expectedCount = 1
        ).map { dataPoints =>
          assertTrue(
            dataPoints.size == 1,
            findDp(dataPoints, 10, "actuator").exists(_.data == dataJson(5.0))
          )
        }
      }.provideSomeLayer[Scope](
        layers(
          Set(
            flowConfig(
              1,
              "single-averager",
              FlowConfiguration.Processor(
                unit = "Averager",
                parameters = Json.Obj(),
                inbound = Chunk(Address(1, "sensorA", "outA", "inputA"), Address(2, "sensorB", "outB", "inputB")),
                outbound = Chunk(Address(10, "actuator", "in", "output")),
                graphId = "graphIdIsolated1"
              )
            )
          )
        )
      ),
      test("SplitTransform: one inlet, two outlets") {
        // SplitTransform: 6.0 -> doubled=12.0, halved=3.0
        for {
          dataPoints <- sendAndCollect(
                          Chunk(FlatDataPacket(1, "sensor", "out", dataJson(6.0))),
                          expectedCount = 1
                        )
          doubledDp   = findDp(dataPoints, 20, "out-doubled")
          halvedDp    = findDp(dataPoints, 20, "out-halved")
        } yield assertTrue(
          dataPoints.size == 1,
          doubledDp.map(_.data).contains(dataJson(12.0)),
          halvedDp.map(_.data).contains(dataJson(3.0))
        )
      }.provideSomeLayer[Scope](
        layers(
          Set(
            flowConfig(
              2,
              "single-split",
              FlowConfiguration.Processor(
                unit = "SplitTransform",
                parameters = Json.Obj(),
                inbound = Chunk(Address(1, "sensor", "out", "input")),
                outbound =
                  Chunk(Address(20, "out-doubled", "in", "doubled"), Address(20, "out-halved", "in", "halved")),
                graphId = "graphIdIsolated2"
              )
            )
          )
        )
      ),
      test("SumDiff: two inlets, two outlets with parameters") {
        // SumDiff with scale=2.0: sum=(4+6)*2=20, diff=(4-6)*2=-4
        for {
          dataPoints <- sendAndCollect(
                          Chunk(
                            FlatDataPacket(1, "sX", "outX", dataJson(4.0)),
                            FlatDataPacket(2, "sY", "outY", dataJson(6.0))
                          ),
                          expectedCount = 1
                        )
          sumDp       = findDp(dataPoints, 30, "out-sum")
          diffDp      = findDp(dataPoints, 30, "out-diff")
        } yield assertTrue(
          dataPoints.size == 1,
          sumDp.map(_.data).contains(dataJson(20.0)),
          diffDp.map(_.data).contains(dataJson(-4.0))
        )
      }.provideSomeLayer[Scope](
        layers(
          Set(
            flowConfig(
              3,
              "single-sumdiff",
              FlowConfiguration.Processor(
                unit = "SumDiff",
                parameters = Json.Obj("scale" -> Json.Num(2.0)),
                inbound = Chunk(Address(1, "sX", "outX", "inputX"), Address(2, "sY", "outY", "inputY")),
                outbound = Chunk(Address(30, "out-sum", "in", "sum"), Address(30, "out-diff", "in", "diff")),
                graphId = "graphIdIsolated3"
              )
            )
          )
        )
      )
    ),
    suite("Two processors - isolated addresses")(
      test("Averager and SplitTransform with non-intersecting addresses") {
        for {
          // Averager: (10+20)/2 = 15
          avgDps   <- sendAndCollect(
                        Chunk(
                          FlatDataPacket(1, "a1", "outA", dataJson(10.0)),
                          FlatDataPacket(2, "a2", "outB", dataJson(20.0))
                        ),
                        expectedCount = 1
                      )
          // SplitTransform: 8.0 -> doubled=16, halved=4
          splitDps <- sendAndCollect(
                        Chunk(FlatDataPacket(5, "s1", "out", dataJson(8.0))),
                        expectedCount = 1
                      )
        } yield assertTrue(
          avgDps.size == 1,
          avgDps.head.rest.size == 1,
          findDp(avgDps, 10, "avg-out").exists(_.data == dataJson(15.0)),
          splitDps.size == 1,
          splitDps.head.rest.size == 2,
          findDp(splitDps, 20, "d").exists(_.data == dataJson(16.0)),
          findDp(splitDps, 20, "h").exists(_.data == dataJson(4.0))
        )
      }.provideSomeLayer[Scope](
        layers(
          Set(
            flowConfig(
              4,
              "two-isolated",
              FlowConfiguration.Processor(
                unit = "Averager",
                parameters = Json.Obj(),
                inbound = Chunk(Address(1, "a1", "outA", "inputA"), Address(2, "a2", "outB", "inputB")),
                outbound = Chunk(Address(10, "avg-out", "in", "output")),
                graphId = "graphIdIsolated1"
              ),
              FlowConfiguration.Processor(
                unit = "SplitTransform",
                parameters = Json.Obj(),
                inbound = Chunk(Address(5, "s1", "out", "input")),
                outbound = Chunk(Address(20, "d", "in", "doubled"), Address(20, "h", "in", "halved")),
                graphId = "graphIdIsolated2"
              )
            )
          )
        )
      )
    ),
    suite("Two processors - shared inbound address")(
      test("SplitTransform and SumDiff sharing one inbound sensor") {
        // Both processors read from (1, "shared"). SplitTransform uses it as its sole input.
        // SumDiff uses it as inputX; inputY comes from (2, "other").
        // Input: shared=5.0, other=3.0
        // SplitTransform: 5.0 -> doubled=10, halved=2.5
        // SumDiff(scale=1): sum=5+3=8, diff=5-3=2
        for {
          dataPoints <- sendAndCollect(
                          Chunk(
                            FlatDataPacket(1, "shared", "out", dataJson(5.0)),
                            FlatDataPacket(2, "other", "out", dataJson(3.0))
                          ),
                          expectedCount = 2
                        )
          // SplitTransform outputs
          doubled     = findDp(dataPoints, 40, "d")
          halved      = findDp(dataPoints, 40, "h")
          // SumDiff outputs
          sumDp       = findDp(dataPoints, 50, "s")
          diffDp      = findDp(dataPoints, 50, "df")
        } yield assertTrue(
          doubled.exists(_.data == dataJson(10.0)),
          halved.exists(_.data == dataJson(2.5)),
          sumDp.exists(_.data == dataJson(8.0)),
          diffDp.exists(_.data == dataJson(2.0))
        )
      }.provideSomeLayer[Scope](
        layers(
          Set(
            flowConfig(
              5,
              "two-shared-inbound",
              FlowConfiguration.Processor(
                unit = "SplitTransform",
                parameters = Json.Obj(),
                inbound = Chunk(Address(1, "shared", "out", "input")),
                outbound = Chunk(Address(40, "d", "in", "doubled"), Address(40, "h", "in", "halved")),
                graphId = "graphIdShared1"
              ),
              FlowConfiguration.Processor(
                unit = "SumDiff",
                parameters = Json.Obj("scale" -> Json.Num(1.0)),
                inbound = Chunk(Address(1, "shared", "out", "inputX"), Address(2, "other", "out", "inputY")),
                outbound = Chunk(Address(50, "s", "in", "sum"), Address(50, "df", "in", "diff")),
                graphId = "graphIdShared2"
              )
            )
          )
        )
      )
    ),
    suite("Three processors - isolated addresses")(
      test("all three processors with non-intersecting addresses") {
        for {
          // Averager: (4+8)/2 = 6
          avgDps   <- sendAndCollect(
                        Chunk(
                          FlatDataPacket(1, "a1", "in1", dataJson(4.0)),
                          FlatDataPacket(2, "a2", "in2", dataJson(8.0))
                        ),
                        expectedCount = 1
                      )
          // SplitTransform: 10 -> doubled=20, halved=5
          splitDps <- sendAndCollect(
                        Chunk(FlatDataPacket(3, "st", "in", dataJson(10.0))),
                        expectedCount = 1
                      )
          // SumDiff(scale=0.5): sum=(7+3)*0.5=5, diff=(7-3)*0.5=2
          sdDps    <- sendAndCollect(
                        Chunk(
                          FlatDataPacket(5, "x", "in", dataJson(7.0)),
                          FlatDataPacket(6, "y", "in", dataJson(3.0))
                        ),
                        expectedCount = 1
                      )
        } yield assertTrue(
          avgDps.size == 1,
          avgDps.head.rest.size == 1,
          avgDps.head.rest.head._2 == Map("out" -> dataJson(6.0)),
          splitDps.exists((dp: PackedDataPacket) => dp.rest("dbl") == Map("out" -> dataJson(20.0))),
          splitDps.exists((dp: PackedDataPacket) => dp.rest("hlf") == Map("out" -> dataJson(5.0))),
          sdDps.exists((dp: PackedDataPacket) => dp.rest("sm") == Map("out2" -> dataJson(5.0))),
          sdDps.exists((dp: PackedDataPacket) => dp.rest("df") == Map("out2" -> dataJson(2.0)))
        )
      }.provideSomeLayer[Scope](
        layers(
          Set(
            flowConfig(
              6,
              "three-isolated",
              FlowConfiguration.Processor(
                unit = "Averager",
                parameters = Json.Obj(),
                inbound = Chunk(Address(1, "a1", "in1", "inputA"), Address(2, "a2", "in2", "inputB")),
                outbound = Chunk(Address(60, "avg", "out", "output")),
                graphId = "graphIdIsolated1"
              ),
              FlowConfiguration.Processor(
                unit = "SplitTransform",
                parameters = Json.Obj(),
                inbound = Chunk(Address(3, "st", "in", "input")),
                outbound = Chunk(Address(61, "dbl", "out", "doubled"), Address(61, "hlf", "out", "halved")),
                graphId = "graphIdIsolated2"
              ),
              FlowConfiguration.Processor(
                unit = "SumDiff",
                parameters = Json.Obj("scale" -> Json.Num(0.5)),
                inbound = Chunk(Address(5, "x", "in", "inputX"), Address(6, "y", "in", "inputY")),
                outbound = Chunk(Address(62, "sm", "out2", "sum"), Address(62, "df", "out2", "diff")),
                graphId = "graphIdIsolated3"
              )
            )
          )
        )
      )
    ),
    suite("Three processors - partially shared addresses")(
      test("two processors share an inbound address, third is independent") {
        // Averager reads from (1, "common") as inputA and (2, "solo-a") as inputB
        // SumDiff reads from (1, "common") as inputX and (3, "solo-b") as inputY
        // SplitTransform reads from (4, "independent") alone
        // Input: common=6, solo-a=4, solo-b=2, independent=10
        // Averager: (6+4)/2 = 5
        // SumDiff(scale=1): sum=6+2=8, diff=6-2=4
        // SplitTransform: 10 -> doubled=20, halved=5
        for {
          dataPoints <- sendAndCollect(
                          Chunk(
                            FlatDataPacket(1, "common", "in", dataJson(6.0)),
                            FlatDataPacket(2, "solo-a", "in", dataJson(4.0)),
                            FlatDataPacket(3, "solo-b", "in", dataJson(2.0)),
                            FlatDataPacket(4, "independent", "in", dataJson(10.0))
                          ),
                          expectedCount = 3
                        )
          // Averager output
          avgDp       = findDp(dataPoints, 70, "avg")
          // SumDiff outputs
          sumDp       = findDp(dataPoints, 71, "sm")
          diffDp      = findDp(dataPoints, 71, "df")
          // SplitTransform outputs
          dblDp       = findDp(dataPoints, 72, "dbl")
          hlfDp       = findDp(dataPoints, 72, "hlf")
        } yield assertTrue(
          avgDp.exists(_.data == dataJson(5.0)),
          sumDp.exists(_.data == dataJson(8.0)),
          diffDp.exists(_.data == dataJson(4.0)),
          dblDp.exists(_.data == dataJson(20.0)),
          hlfDp.exists(_.data == dataJson(5.0))
        )
      }.provideSomeLayer[Scope](
        layers(
          Set(
            flowConfig(
              7,
              "three-partial-shared",
              FlowConfiguration.Processor(
                unit = "Averager",
                parameters = Json.Obj(),
                inbound = Chunk(Address(1, "common", "in", "inputA"), Address(2, "solo-a", "in", "inputB")),
                outbound = Chunk(Address(70, "avg", "out", "output")),
                graphId = "graphIdPartial1"
              ),
              FlowConfiguration.Processor(
                unit = "SumDiff",
                parameters = Json.Obj("scale" -> Json.Num(1.0)),
                inbound = Chunk(Address(1, "common", "in", "inputX"), Address(3, "solo-b", "in", "inputY")),
                outbound = Chunk(Address(71, "sm", "out", "sum"), Address(71, "df", "out", "diff")),
                graphId = "graphIdPartial2"
              ),
              FlowConfiguration.Processor(
                unit = "SplitTransform",
                parameters = Json.Obj(),
                inbound = Chunk(Address(4, "independent", "in", "input")),
                outbound = Chunk(Address(72, "dbl", "out", "doubled"), Address(72, "hlf", "out", "halved")),
                graphId = "graphIdPartial3"
              )
            )
          )
        )
      ),
      test("all three processors share the same inbound address") {
        // All read from (1, "sensor") — Averager uses it for both inputA and inputB,
        // SumDiff uses it for both inputX and inputY, SplitTransform uses it as input.
        // Input: sensor=8
        // Averager: (8+8)/2 = 8
        // SumDiff(scale=2): sum=(8+8)*2=32, diff=(8-8)*2=0
        // SplitTransform: 8 -> doubled=16, halved=4
        for {
          dataPoints <- sendAndCollect(
                          Chunk(FlatDataPacket(1, "sensor", "in", dataJson(8.0))),
                          expectedCount = 3
                        )
          avgDp       = findDp(dataPoints, 80, "avg")
          sumDp       = findDp(dataPoints, 81, "sm")
          diffDp      = findDp(dataPoints, 81, "df")
          dblDp       = findDp(dataPoints, 82, "dbl")
          hlfDp       = findDp(dataPoints, 82, "hlf")
        } yield assertTrue(
          avgDp.exists(_.data == dataJson(8.0)),
          sumDp.exists(_.data == dataJson(32.0)),
          diffDp.exists(_.data == dataJson(0.0)),
          dblDp.exists(_.data == dataJson(16.0)),
          hlfDp.exists(_.data == dataJson(4.0))
        )
      }.provideSomeLayer[Scope](
        layers(
          Set(
            flowConfig(
              8,
              "three-all-shared",
              FlowConfiguration.Processor(
                unit = "Averager",
                parameters = Json.Obj(),
                inbound = Chunk(Address(1, "sensor", "in", "inputA"), Address(1, "sensor", "in", "inputB")),
                outbound = Chunk(Address(80, "avg", "out", "output")),
                graphId = "graphIdAllShared1"
              ),
              FlowConfiguration.Processor(
                unit = "SumDiff",
                parameters = Json.Obj("scale" -> Json.Num(2.0)),
                inbound = Chunk(Address(1, "sensor", "in", "inputX"), Address(1, "sensor", "in", "inputY")),
                outbound = Chunk(Address(81, "sm", "out", "sum"), Address(81, "df", "out", "diff")),
                graphId = "graphIdAllShared2"
              ),
              FlowConfiguration.Processor(
                unit = "SplitTransform",
                parameters = Json.Obj(),
                inbound = Chunk(Address(1, "sensor", "in", "input")),
                outbound = Chunk(Address(82, "dbl", "out", "doubled"), Address(82, "hlf", "out", "halved")),
                graphId = "graphIdAllShared3"
              )
            )
          )
        )
      )
    )
  ) @@ TestAspect.sequential
}
