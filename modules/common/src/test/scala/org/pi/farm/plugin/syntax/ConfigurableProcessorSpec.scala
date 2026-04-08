package org.pi.farm.plugin.syntax

import zio.*
import zio.stream.*
import zio.test.*
import zio.test.Assertion.*
import zio.json.*
import zio.json.ast.Json
import org.pi.farm.model.*
import org.pi.farm.model.Message.*
import org.pi.farm.model.given
import org.pi.farm.plugin.{Inlet, Outlet}
import scala.language.implicitConversions
import org.pi.farm.plugin.Processor

object ConfigurableProcessorSpec extends ZIOSpecDefault {

  trait P extends Processor {
    case class Params(factor: Int)
    type ParamsType = Params
    given paramsCodec: JsonCodec[Params]        = zio.json.DeriveJsonCodec.gen[Params]
    val paramsSchema: zio.schema.Schema[Params] = zio.schema.DeriveSchema.gen[Params]

  }

  val inletA: Inlet[Int]      = Inlet[Int]("a", "celsius")
  val inletB: Inlet[String]   = Inlet[String]("b", "label")
  val outletX: Outlet[Int]    = Outlet[Int]("x", "watts")
  val outletY: Outlet[String] = Outlet[String]("y", "status")

  val cid1: ControllerId = 1
  val cid2: ControllerId = 2
  val pid1: PeripheryId  = "p1"
  val pid2: PeripheryId  = "p2"
  val pid3: PeripheryId  = "p3"

  def mkConfig(
    inbound: Chunk[Address] = Chunk.empty,
    outbound: Chunk[Address] = Chunk.empty,
    params: Json = Json.Obj("factor" -> Json.Num(2))
  ): Configuration =
    Configuration(
      id = 1,
      name = "test",
      description = "",
      inbound = inbound,
      outbound = outbound,
      processingUnit = "test",
      additional = params
    )

  def mkDataPacket[T: JsonCodec](cid: ControllerId, pid: PeripheryId, value: T): DataPacket =
    DataPacket(cid, pid, Data(value).toJsonAST.toOption.get)

  /** Run an Inbound message through a configured pipeline, collect outputs */
  def runPipeline(
    cp: ConfigurableProcessor.Aux[Any],
    config: Configuration,
    messages: Chunk[Inbound]
  )(using t: zio.Trace): ZStream[Any, Throwable, Outbound] =
    ZStream.unwrap {
      cp.configure(config).map { pipeline =>
        ZStream
          .fromChunk(messages)
          .via(pipeline)
      }
    }

  def spec = suite("ConfigurableProcessor")(
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
        Pp.work.configure(config).flip.map(e => assertTrue(e.getMessage.contains("Failed to decode")))
      },
      test("ConsumerProcessor fails on not configured inlet") {
        object Pp extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] = ZIO.unit

          val work = from(inletA).consumeBy(process)
        }
        // 2 addresses for 1 inlet → groupByControllerId collapses them
        // but here use 0 addresses for 1 inlet
        val config = mkConfig(inbound = Chunk.empty)
        Pp.work.configure(config).flip.map(e => assertTrue(e.getMessage.contains("Missing inlets in configuration: a")))
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
          .configure(config)
          .flip
          .map(e => assertTrue(e.getMessage.contains("Excessive inlets in configuration: b")))
      }
    ),

    // --- ConsumerProcessor ---
    suite("ConsumerProcessor")(
      test("processes matching DataPacket and emits nothing") {
        class Pp(ref: Ref[Option[Int]]) extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] = ref.set(Some(in))

          val work: ConfigurableProcessor.Aux[Any] = from(inletA).consumeBy(process)
        }
        val config = mkConfig(inbound = Chunk(Address(cid1, pid1, "a")))
        for {
          received <- Ref.make[Option[Int]](None)
          pp = new Pp(received)
          out <- runPipeline(pp.work, config, Chunk(mkDataPacket(cid1, pid1, 42))).runCollect
          got <- received.get
        } yield assertTrue(out.isEmpty, got == Some(42))
      },
      test("ignores DataPacket not matching any inlet") {
        class Pp(ref: Ref[Boolean]) extends P {
          def process(in: Int)(using params: ParamsType): UIO[Unit] = ref.set(true)

          val work: ConfigurableProcessor.Aux[Any] = from(inletA).consumeBy(process)
        }

        val config = mkConfig(inbound = Chunk(Address(cid1, pid1, "a")))
        for {
          called <- Ref.make(false)
          pp = new Pp(called)
          out <- runPipeline(pp.work, config, Chunk(mkDataPacket(cid2, pid2, 99))).runCollect
          c   <- called.get
        } yield assertTrue(out.isEmpty, !c)
      }
    ),

    // --- InOutPProcessor ---
    suite("InOutPProcessor")(
      test("transforms input to Command output") {
        object Pp extends P {
          def proc(in: Int)(using params: ParamsType): Int = in * params.factor

          val work: ConfigurableProcessor.Aux[Any] = from(inletA).to(outletX).via(proc)
        }
        val config = mkConfig(
          inbound = Chunk(Address(cid1, pid1, "a")),
          outbound = Chunk(Address(cid2, pid3, "x"))
        )
        runPipeline(Pp.work, config, Chunk(mkDataPacket(cid1, pid1, 21))).runCollect.map { out =>
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

          val work: ConfigurableProcessor.Aux[Any] = to(outletX).from(proc)
        }
        val config = mkConfig(outbound = Chunk(Address(cid1, pid1, "x")))
        for {
          pipeline <- Pp.work.configure(config)
          // OutPProcessor uses ZChannel.fromZIO — needs a dummy input to trigger
          out <- ZStream(mkDataPacket(cid1, pid1, 0): Inbound)
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

          val work: ConfigurableProcessor.Aux[Any] = from(inletB, inletA).to(outletX, outletY).viaZIO(proc)
        }
        val config = mkConfig(
          inbound = Chunk(Address(cid1, pid1, "a"), Address(cid1, pid2, "b")),
          outbound = Chunk(Address(cid1, pid3, "x"), Address(cid2, pid2, "y"))
        )
        runPipeline(Pp.work, config, Chunk(mkDataPacket(cid1, pid1, 5), mkDataPacket(cid1, pid2, "hello"))).runCollect
          .map { out =>
            val cmdX = out.collect { case m @ Message.Command(controllerId, _) if controllerId == cid1 => m }
            val cmdY = out.collect { case m @ Message.Command(controllerId, _) if controllerId == cid2 => m }
            assertTrue(
              cmdX.flatMap(_.dataPoints).size == 1,
              cmdX.flatMap(_.dataPoints).head.peripheryId == pid3,
              cmdX.flatMap(_.dataPoints).head.data.as[Data[Int]].toOption.get.value == 10,
              cmdY.flatMap(_.dataPoints).size == 1,
              cmdY.flatMap(_.dataPoints).head.peripheryId == pid2,
              cmdY.flatMap(_.dataPoints).head.data.as[Data[String]].toOption.get.value == "olleh"
            )
          }
      }
    )
  )
}
