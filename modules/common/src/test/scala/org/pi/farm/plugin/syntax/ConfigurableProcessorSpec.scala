package org.pi.farm.plugin.syntax

import org.pi.farm.PiFarmSpec
import org.pi.farm.generators.ModelGenerators.descriptionGen
import org.pi.farm.model.{*, given}
import org.pi.farm.model.Message.*
import org.pi.farm.plugin.{DataProcessor, Inlet, Outlet}
import org.pi.farm.plugin.macros.processor
import org.pi.farm.runtime.*

import zio.*
import zio.config.magnolia.examples.C
import zio.json.*
import zio.json.ast.Json
import zio.stream.*
import zio.test.*
import zio.test.Assertion.*

import scala.language.implicitConversions

import cats.data.NonEmptySet

object ConfigurableProcessorSpec extends PiFarmSpec {

  extension (data: DataPacket) {
    def value[T](using JsonDecoder[Data[T]]): T = data.data.as[Data[T]].toOption.get.value
  }

  trait P extends DataProcessor {
    case class Params(factor: Int)
    type ParamsType = Params
    given paramsCodec: JsonCodec[Params]         = zio.json.DeriveJsonCodec.gen[Params]
    val processorDefinition: ProcessorDefinition = ProcessorDefinition(
      name = "Test Processor",
      description = "A processor for testing",
      paramsSchema = Json.Null,
      inbound = Chunk.empty,
      outbound = Chunk.empty
    )
  }

  val inletA: Inlet[Int]      = Inlet[Int]("a", "celsius")
  val inletB: Inlet[String]   = Inlet[String]("b", "label")
  val outletX: Outlet[Int]    = Outlet[Int]("x", "watts")
  val outletY: Outlet[String] = Outlet[String]("y", "status")

  val cid1: ControllerId = 1
  val cid2: ControllerId = 2
  val cid3: ControllerId = 3
  val cid4: ControllerId = 4
  val cid5: ControllerId = 5
  val cid6: ControllerId = 6

  val pid1: PeripheryId = "p1"
  val pid2: PeripheryId = "p2"
  val pid3: PeripheryId = "p3"

  def mkConfig(
    inbound: Chunk[Address] = Chunk.empty,
    outbound: Chunk[Address] = Chunk.empty,
    params: Json = Json.Obj("factor" -> Json.Num(2))
  ): FlowConfiguration =
    FlowConfiguration(
      id = 1,
      name = "test",
      description = "",
      processors = NonEmptySet.one(FlowConfiguration.Processor("test", params, inbound, outbound))
    )

  def mkDataPacket[T: JsonCodec](cid: ControllerId, pid: PeripheryId, value: T): DataPacket =
    DataPacket(cid, pid, Data(value).toJsonAST.toOption.get)

  /** Run an Inbound message through a configured pipeline, collect outputs */
  def runPipeline(
    cp: ConfigurableFlow.Aux[Any],
    config: FlowConfiguration,
    messages: Inbound*
  )(using t: zio.Trace): Task[Chunk[Outbound]] = ZIO.scoped {
    val stream    = ZStream.fromIterable(messages)
    val broadcast = stream.broadcastDynamic(1)
    for {
      pipelines <- ZIO.foreach(Chunk.from(config.processors))(cp.configure)
      streams   <- ZIO.foreach(pipelines) { p => broadcast.map(_.via(p)) }
      res       <- ZStream.mergeAllUnbounded()(streams*).runCollect
    } yield res
  }

  def spec = suite("ConfigurableProcessor")(
    suite("@processor annotation")(
      test("generates correct ProcessorDefinition") {
        @processor(
          name = "Test Processor",
          description = "A processor for testing"
        )
        object Pp extends DataProcessor {
          case class Params(factor: Float)
          type ParamsType = Params
          given paramsCodec: JsonCodec[Params] = DeriveJsonCodec.gen[Params]

          val inletA  = Inlet[Int]("a", "celsius")
          val outletX = Outlet[Double]("x", "watts")

          def process(in: Int)(using params: ParamsType): Double = in.toDouble * params.factor.toDouble

          val work = from(inletA).to(outletX).via(process)
        }
        assertTrue(
          Pp.processorDefinition.name == summon[Conversion[String, Name]]("Test Processor"),
          Pp.processorDefinition.description == "A processor for testing",
          Pp.processorDefinition.paramsSchema == Json.Obj("factor" -> Json.Str("Float")),
          Pp.processorDefinition.inbound == Chunk(
            ProcessorDefinition.InputConnection(name = "a", units = "celsius", `type` = "Int", description = "")
          ),
          Pp.processorDefinition.outbound == Chunk(
            ProcessorDefinition.OutputConnection(name = "x", units = "watts", `type` = "Double", description = "")
          )
        )
      }
    ),
    // --- Configuration validation ---
    suite("configuration validation")(
      test("fails on invalid params JSON") {
        object Pp extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] = ZIO.unit

          val work = from(inletA).consumeBy(process)
        }
        val config = mkConfig(
          inbound = Chunk(Address(cid1, pid1, "a")),
          params = Json.Str("not-a-params")
        )
        Pp.work.configure(config.processors.head).flip.map(e => assertTrue(e.getMessage.contains("Failed to decode")))
      },
      test("ConsumerProcessor fails on not configured inlet") {
        object Pp extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] = ZIO.unit

          val work = from(inletA).consumeBy(process)
        }
        // 2 addresses for 1 inlet → groupByControllerId collapses them
        // but here use 0 addresses for 1 inlet
        val config = mkConfig(inbound = Chunk.empty)
        Pp.work
          .configure(config.processors.head)
          .flip
          .map(e => assertTrue(e.getMessage.contains("Missing inlets in configuration: a")))
      },
      test("ConsumerProcessor fails on excessive inlet in configuration") {
        object Pp extends P {
          def process(using params: Params)(in: Int): UIO[Unit] = ZIO.unit

          val work = from(inletA).consumeBy(process)
        }
        // 2 addresses for 1 inlet → groupByControllerId collapses them
        // but here use 0 addresses for 1 inlet
        val config = mkConfig(inbound = Chunk(Address(cid1, pid1, "a"), Address(cid2, pid2, "b")))
        Pp.work
          .configure(config.processors.head)
          .flip
          .map(e => assertTrue(e.getMessage.contains("Excessive inlets in configuration: b")))
      }
    ),

    // --- ConsumerProcessor ---
    suite("ConsumerProcessor")(
      test("processes matching DataPacket and emits nothing") {
        class Pp(ref: Ref[Option[Int]]) extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] = ref.set(Some(in))

          val work: ConfigurableFlow.Aux[Any] = from(inletA).consumeBy(process)
        }
        val config = mkConfig(inbound = Chunk(Address(cid1, pid1, "a")))
        for {
          received <- Ref.make[Option[Int]](None)
          pp        = new Pp(received)
          out      <- runPipeline(pp.work, config, mkDataPacket(cid1, pid1, 42))
          got      <- received.get
        } yield assertTrue(out.isEmpty, got == Some(42))
      },
      test("ignores DataPacket not matching any inlet") {
        class Pp(ref: Ref[Boolean]) extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] = ref.set(true)

          val work: ConfigurableFlow.Aux[Any] = from(inletA).consumeBy(process)
        }

        val config = mkConfig(inbound = Chunk(Address(cid1, pid1, "a")))
        for {
          called <- Ref.make(false)
          pp      = new Pp(called)
          out    <- runPipeline(pp.work, config, mkDataPacket(cid2, pid2, 99))
          c      <- called.get
        } yield assertTrue(out.isEmpty, !c)
      }
    ),

    // --- InOutPProcessor ---
    suite("InOutPProcessor")(
      test("transforms input to Command output") {
        object Pp extends P {
          def proc(in: Int)(using params: ParamsType): Int = in * params.factor

          val work: ConfigurableFlow.Aux[Any] = from(inletA).to(outletX).via(proc)
        }
        val config = mkConfig(
          inbound = Chunk(Address(cid1, pid1, "a")),
          outbound = Chunk(Address(cid2, pid3, "x"))
        )
        runPipeline(Pp.work, config, mkDataPacket(cid1, pid1, 21)).map { out =>
          val cmd = out.head.asInstanceOf[Command]
          assertTrue(
            cmd.controllerId == cid2,
            cmd.dataPoints.size == 1,
            cmd.dataPoints.head.peripheryId == pid3
          )
        }
      }
    ),

    // --- OutPProcessor ---
    suite("OutPProcessor")(
      test("generates output without meaningful input") {
        object Pp extends P {
          def proc(using params: ParamsType): ZStream[Any, Nothing, Int] = ZStream.succeed(params.factor * 10)

          val work: ConfigurableFlow.Aux[Any] = to(outletX).from(proc)
        }
        val config = mkConfig(outbound = Chunk(Address(cid1, pid1, "x")))
        for {
          pipeline <- Pp.work.configure(config.processors.head)
          // OutPProcessor uses ZChannel.fromZIO — needs a dummy input to trigger
          out      <- ZStream(mkDataPacket(cid1, pid1, 0): Inbound)
                        .via(pipeline.asInstanceOf[ZPipeline[Any, Throwable, Inbound, Outbound]])
                        .runCollect
        } yield {
          val cmd = out.head.asInstanceOf[Command]
          assertTrue(cmd.controllerId == cid1)
        }
      }
    ),

    suite("Multiple inlets and outlets")(
      test("processes multiple inlets and outlets") {
        object Pp extends P {
          def proc(str: String, int: Int)(using params: ParamsType): UIO[(Int, String)] =
            ZIO.succeed((int * params.factor, str.reverse))

          val work: ConfigurableFlow.Aux[Any] = from(inletB, inletA).to(outletX, outletY).viaZIO(proc)
        }
        val config = mkConfig(
          inbound = Chunk(Address(cid1, pid1, "a"), Address(cid1, pid2, "b")),
          outbound = Chunk(Address(cid1, pid3, "x"), Address(cid2, pid2, "y"))
        )
        runPipeline(Pp.work, config, mkDataPacket(cid1, pid1, 5), mkDataPacket(cid1, pid2, "hello"))
          .map { out =>
            val cmdX = out.collect { case m @ Message.Command(controllerId, _) if controllerId == cid1 => m }
            val cmdY = out.collect { case m @ Message.Command(controllerId, _) if controllerId == cid2 => m }
            assertTrue(
              cmdX.flatMap(_.dataPoints).size == 1,
              cmdX.flatMap(_.dataPoints).head.peripheryId == pid3,
              cmdX.flatMap(_.dataPoints).head.value[Int] == 10,
              cmdY.flatMap(_.dataPoints).size == 1,
              cmdY.flatMap(_.dataPoints).head.peripheryId == pid2,
              cmdY.flatMap(_.dataPoints).head.value[String] == "olleh"
            )
          }
      }
    ),

    // --- Multiple processors in a single configuration ---
    suite("Multiple processors in a configuration")(
      test("2 processors each process their own inbound data independently") {
        object Pp extends P {
          def proc(in: Int)(using params: ParamsType): Int = in * params.factor

          val work: ConfigurableFlow.Aux[Any] = from(inletA).to(outletX).via(proc)
        }
        val config = FlowConfiguration(
          id = 1,
          name = "two-processors",
          description = "",
          processors = NonEmptySet.of(
            FlowConfiguration.Processor(
              "proc1",
              Json.Obj("factor" -> Json.Num(2)),
              Chunk(Address(cid1, pid1, "a")),
              Chunk(Address(cid2, pid1, "x"))
            ),
            FlowConfiguration.Processor(
              "proc2",
              Json.Obj("factor" -> Json.Num(3)),
              Chunk(Address(cid3, pid2, "a")),
              Chunk(Address(cid4, pid2, "x"))
            )
          )
        )
        runPipeline(
          Pp.work,
          config,
          mkDataPacket(cid3, pid2, 10),
          mkDataPacket(cid1, pid1, 1)
        )
          .map { out =>
            val cmds   = out.collect { case m: Command => m }
            val toCid2 = cmds.filter(_.controllerId == cid2)
            val toCid4 = cmds.filter(_.controllerId == cid4)
            assertTrue(
              toCid2.size == 1,
              toCid2.head.dataPoints.head.value[Int] == 2, // 1 * 2
              toCid4.size == 1,
              toCid4.head.dataPoints.head.value[Int] == 30 // 10 * 3
            )
          }
      },
      test("2 processors ignore data not matching their inbound addresses") {
        class Pp(ref: Ref[Int]) extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] =
            ZIO.logInfo("Got one") *> ref.update(_ + 1)

          val work: ConfigurableFlow.Aux[Any] = from(inletA).consumeBy(process)
        }
        val config = FlowConfiguration(
          id = 1,
          name = "two-consumers",
          description = "",
          processors = NonEmptySet.of(
            FlowConfiguration
              .Processor("proc1", Json.Obj("factor" -> Json.Num(1)), Chunk(Address(cid1, pid1, "a")), Chunk.empty),
            FlowConfiguration.Processor(
              "proc2",
              Json.Obj("factor" -> Json.Num(1)),
              Chunk(Address(cid2, pid1, "a")),
              Chunk.empty
            )
          )
        )
        for {
          count <- Ref.make(0)
          pp     = new Pp(count)
          // Send data only to cid1 — only proc1 should fire
          _     <- runPipeline(pp.work, config, mkDataPacket(cid1, pid1, 42))
          c     <- count.get
        } yield assertTrue(c == 1)
      },
      test("3 processors each produce independent outputs") {
        object Pp extends P {
          def proc(in: Int)(using params: ParamsType): Int = in * params.factor

          val work: ConfigurableFlow.Aux[Any] = from(inletA).to(outletX).via(proc)
        }
        val config = FlowConfiguration(
          id = 1,
          name = "three-processors",
          description = "",
          processors = NonEmptySet.of(
            FlowConfiguration.Processor(
              "proc1",
              Json.Obj("factor" -> Json.Num(2)),
              Chunk(Address(cid1, pid1, "a")),
              Chunk(Address(cid2, pid2, "x"))
            ),
            FlowConfiguration.Processor(
              "proc2",
              Json.Obj("factor" -> Json.Num(3)),
              Chunk(Address(cid3, pid1, "a")),
              Chunk(Address(cid4, pid2, "x"))
            ),
            FlowConfiguration.Processor(
              "proc3",
              Json.Obj("factor" -> Json.Num(5)),
              Chunk(Address(cid5, pid1, "a")),
              Chunk(Address(cid6, pid2, "x"))
            )
          )
        )
        runPipeline(
          Pp.work,
          config,
          mkDataPacket(cid1, pid1, 7),
          mkDataPacket(cid3, pid1, 7),
          mkDataPacket(cid5, pid1, 7)
        )
          .map { out =>
            val cmds   = out.collect { case m: Command => m }
            val toCid2 = cmds.filter(_.controllerId == cid2)
            val toCid4 = cmds.filter(_.controllerId == cid4)
            val toCid6 = cmds.filter(_.controllerId == cid6)
            assertTrue(
              toCid2.size == 1,
              toCid2.head.dataPoints.head.value[Int] == 14, // 7 * 2
              toCid4.size == 1,
              toCid4.head.dataPoints.head.value[Int] == 21, // 7 * 3
              toCid6.size == 1,
              toCid6.head.dataPoints.head.value[Int] == 35  // 7 * 5
            )
          }
      },
      test("3 processors - partial data triggers only matching processors") {
        class Pp(ref: Ref[List[Int]]) extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] = ref.update(_ :+ (in * params.factor))

          val work: ConfigurableFlow.Aux[Any] = from(inletA).consumeBy(process)
        }
        val config = FlowConfiguration(
          id = 1,
          name = "three-consumers",
          description = "",
          processors = NonEmptySet.of(
            FlowConfiguration
              .Processor("proc1", Json.Obj("factor" -> Json.Num(1)), Chunk(Address(cid1, pid1, "a")), Chunk.empty),
            FlowConfiguration
              .Processor("proc2", Json.Obj("factor" -> Json.Num(10)), Chunk(Address(cid3, pid1, "a")), Chunk.empty),
            FlowConfiguration.Processor(
              "proc3",
              Json.Obj("factor" -> Json.Num(100)),
              Chunk(Address(cid5, pid1, "a")),
              Chunk.empty
            )
          )
        )
        for {
          results <- Ref.make[List[Int]](Nil)
          pp       = new Pp(results)
          // Only send data to cid1 and cid5 — proc1 and proc3 fire, proc2 doesn't
          _       <- runPipeline(pp.work, config, mkDataPacket(cid1, pid1, 5), mkDataPacket(cid5, pid1, 5))
          got     <- results.get
        } yield assertTrue(
          got.contains(5),   // 5 * 1 from proc1
          got.contains(500), // 5 * 100 from proc3
          !got.contains(50)  // proc2 not triggered
        )
      },
      test("3 processors with 2 inputs and 2 outputs each, overlapping addresses") {
        // proc(str, int) → (int * factor, str.reverse)
        // Overlapping inputs:
        //   (cid1,pid1) feeds proc1 and proc2 as "a" (Int)
        //   (cid2,pid1) feeds proc1 and proc3 as "b" (String)
        // Overlapping outputs:
        //   (cid3,pid1) receives from proc1 "x", proc2 "x", and proc3 "y"
        //   (cid4,pid1) receives from proc1 "y" and proc3 "x"
        object Pp extends P {
          def proc(str: String, int: Int)(using params: ParamsType): UIO[(Int, String)] =
            ZIO.succeed((int * params.factor, str.reverse))

          val work: ConfigurableFlow.Aux[Any] = from(inletB, inletA).to(outletX, outletY).viaZIO(proc)
        }
        val config = FlowConfiguration(
          id = 1,
          name = "overlapping",
          description = "",
          processors = NonEmptySet.of(
            // proc1: a=(cid1,pid1), b=(cid2,pid1) → x→(cid3,pid1), y→(cid4,pid1) | factor=2
            FlowConfiguration.Processor(
              "proc1",
              Json.Obj("factor" -> Json.Num(2)),
              Chunk(Address(cid1, pid1, "a"), Address(cid2, pid1, "b")),
              Chunk(Address(cid3, pid1, "x"), Address(cid4, pid1, "y"))
            ),
            // proc2: a=(cid1,pid1) SHARED, b=(cid2,pid2) → x→(cid3,pid1) SHARED, y→(cid5,pid1) | factor=3
            FlowConfiguration.Processor(
              "proc2",
              Json.Obj("factor" -> Json.Num(3)),
              Chunk(Address(cid1, pid1, "a"), Address(cid2, pid2, "b")),
              Chunk(Address(cid5, pid2, "x"), Address(cid4, pid1, "y"))
            ),
            // proc3: a=(cid1,pid2), b=(cid2,pid1) SHARED → x→(cid4,pid1) SHARED, y→(cid3,pid1) SHARED | factor=5
            FlowConfiguration.Processor(
              "proc3",
              Json.Obj("factor" -> Json.Num(5)),
              Chunk(Address(cid1, pid2, "a"), Address(cid2, pid1, "b")),
              Chunk(Address(cid5, pid1, "y"), Address(cid3, pid1, "x"))
            )
          )
        )
        // Send all 4 inputs — triggers all 3 processors
        // proc1: ("foo", 10) → (20, "oof") [(cid3,pid1), (cid4,pid1)]
        // proc2: ("bar", 10) → (30, "rab") [(cid5,pid2), (cid4,pid1)]
        // proc3: ("foo", 20) → (100, "oof") [(cid3,pid1), (cid5,pid1)]
        runPipeline(
          Pp.work,
          config,
          mkDataPacket(cid1, pid1, 10),    // proc1.a, proc2.a
          mkDataPacket(cid2, pid1, "foo"), // proc1.b, proc3.b
          mkDataPacket(cid1, pid2, 20),    // proc3.a
          mkDataPacket(cid2, pid2, "bar")  // proc2.b
        ).map { out =>
          val cmds = out.collect { case m: Command => m }

          // cid3 receives: proc1 x=20(Int), proc2 x=30(Int), proc3 y="oof"(String)
          val toCid3 = cmds.filter(_.controllerId == cid3).flatMap(_.dataPoints)

          // cid4 receives: proc1 y="oof"(String), proc3 x=100(Int)
          val toCid4 = cmds.filter(_.controllerId == cid4).flatMap(_.dataPoints)

          // cid5 receives: proc2 y="rab"(String)
          val toCid5 = cmds.filter(_.controllerId == cid5).flatMap(_.dataPoints)

          val toCid3Pid1 = toCid3.filter(_.peripheryId == pid1)
          val toCid4Pid1 = toCid4.filter(_.peripheryId == pid1)
          val toCid5Pid1 = toCid5.filter(_.peripheryId == pid1)
          val toCid5Pid2 = toCid5.filter(_.peripheryId == pid2)

          assertTrue(
            toCid3Pid1.size == 2,
            toCid3Pid1.map(_.value[Int]).toSet == Set(20, 100),

            toCid4Pid1.size == 2,
            toCid4Pid1.map(_.value[String]).toSet == Set("oof", "rab"),

            toCid5Pid1.size == 1,
            toCid5Pid1.head.value[String] == "oof",

            toCid5Pid2.size == 1,
            toCid5Pid2.head.value[Int] == 30
          )
        }
      }
    )
  )
}
