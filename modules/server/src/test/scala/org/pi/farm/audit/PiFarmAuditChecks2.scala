package org.pi.farm.audit

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.*
import org.pi.farm.model.*
import org.pi.farm.model.Message.Inbound
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.plugin.{Manifest, Service}
import org.pi.farm.processing.{Factory, FactorySpec, FlowConfigurationChanges}
import org.pi.farm.processing.FlowConfigurationChanges.{Add, ChangeQueue, Delete, Update}
import org.pi.farm.runtime.*
import org.pi.farm.service.{FlowConfigurationManager, FlowConfigurationManagerSpec}
import org.pi.farm.storage.*
import org.pi.farm.udp.{QueuesFake, RawMessage}

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.stream.{Take, ZStream}
import zio.test.*

import java.net.InetSocketAddress
import scala.language.implicitConversions

import cats.data.NonEmptySet

object PiFarmAuditChecks2 extends PiFarmSpec {
  private val address      = new InetSocketAddress("127.0.0.1", 12001)
  private val otherAddress = new InetSocketAddress("127.0.0.1", 12002)
  private val first        = Controller(1, 1, "first", "", None)
  private val second       = first.copy(id = 2, name = "second")

  private def processor(unit: String, graphId: String) = FlowConfiguration.Processor(
    unit,
    if (unit == "Factory processor 1") Json.Num(1) else Json.Str("value"),
    Chunk(Address(1, "sensor", "out", "input")),
    Chunk(Address(2, "actuator", "in", "output")),
    graphId
  )

  private def configuration(id: Int, processors: FlowConfiguration.Processor*) = FlowConfiguration(
    id,
    "same name",
    Json.Obj(),
    "",
    NonEmptySet.of(processors.head, processors.tail*),
    None
  )

  private case class TrackingHub(
    hub: Hub[Take[Nothing, Inbound]],
    active: Ref[Int],
    started: Queue[Unit]
  ) extends StreamHub[Inbound] {
    override def subscribe: URIO[Scope, ZStream[Any, Nothing, Inbound]] =
      for {
        stream <- super.subscribe
        _      <- ZIO.acquireRelease(active.update(_ + 1))(_ => active.update(_ - 1))
        _      <- started.offer(())
      } yield stream
  }

  private def withFactory[A](test: (ChangeQueue, TrackingHub) => Task[A]): Task[A] = ZIO.scoped {
    for {
      hub     <- Hub.unbounded[Take[Nothing, Inbound]]
      active  <- Ref.make(0)
      started <- Queue.unbounded[Unit]
      signal   = TrackingHub(hub, active, started)
      changes <- ChangeQueue.make
      manifest = new Manifest {
                   val name                             = "audit"
                   val version                          = "test"
                   val processors                       = Chunk(FactorySpec.IntProcessor, FactorySpec.StringProcessor)
                   val services: Chunk[Service.Creator] = Chunk.empty
                 }
      _       <- ZLayer
                   .makeSome[Scope, Unit](
                     Factory.live,
                     ZLayer.succeed[SignalHub](signal),
                     ResponseQueue.live,
                     UIIncomingQueue.live,
                     UIIncomingHub.live,
                     Controllers.live,
                     ControllerRepositoryFake.empty,
                     ManifestRepository.live(manifest),
                     ProcessingUnitsRepository.live,
                     ConfigurationRepositoryFake.empty,
                     ZLayer.fromFunction((repo: ConfigurationRepository) =>
                       new FlowConfigurationChanges(repo, changes)
                     )
                   )
                   .build
      result  <- test(changes, signal)
    } yield result
  }

  def spec = suite("PiFarm audit checks 2")(
    test("registered sender cannot impersonate another controller") {
      (for {
        controllers    <- ZIO.service[Controllers]
        queues         <- ZIO.service[QueuesFake]
        stream         <- ZIO.service[SignalStream]
        _              <- controllers.addController(address, first)
        _              <- controllers.addController(otherAddress, second)
        forged: Inbound = Message.Measurements(2, Map.empty)
        valid: Inbound  = Message.Measurements(1, Map.empty)
        fiber          <- stream.takeUntil(_ == valid).runCollect.fork
        _              <- queues.incoming.offer(RawMessage(address, forged.toJson))
        _              <- queues.incoming.offer(RawMessage(address, valid.toJson))
        received       <- fiber.join
      } yield assertTrue(received == Chunk(valid))).provide(
        Controllers.live,
        QueuesFake.live,
        SignalStream.live
      )
    },
    test("address reassignment removes obsolete command destinations") {
      (for {
        controllers <- ZIO.service[Controllers]
        _           <- controllers.addController(address, first)
        _           <- controllers.addController(address, second)
        stale       <- controllers.getAddress(first.id)
        _           <- controllers.addController(otherAddress, first)
        owner       <- controllers.getController(address)
        current     <- controllers.getAddress(second.id)
      } yield assertTrue(stale.isEmpty, owner.contains(second), current.contains(address.wrap)))
        .provide(Controllers.live)
    },
    test("concurrent address reassignment keeps controller lookups consistent") {
      (for {
        controllers <- ZIO.service[Controllers]
        _           <- ZIO.foreachParDiscard(1 to 32) { id =>
                         controllers.addController(address, first.copy(id = id.toControllerId))
                       }
        owner       <- controllers.getController(address).someOrFailException
        ids         <- controllers.listControllerIds
        destination <- controllers.getAddress(owner.id)
      } yield assertTrue(ids == List(owner.id), destination.contains(address.wrap)))
        .provide(Controllers.live)
    },
    test("repeated units run independently within and across configurations") {
      withFactory { (changes, signal) =>
        val p = processor("Factory processor 1", "one")
        for {
          _           <- changes.changes.offer(Add(configuration(1, p, p.copy(graphId = "two"))))
          _           <- signal.started.take.repeatN(1)
          withinFlow  <- signal.active.get
          _           <- changes.changes.offer(Add(configuration(2, p)))
          _           <- signal.started.take
          acrossFlows <- signal.active.get
        } yield assertTrue(withinFlow == 2, acrossFlows == 3)
      }
    },
    test("removed processor is stopped after configuration update") {
      withFactory { (changes, signal) =>
        val p   = processor("Factory processor 1", "one")
        val cfg = configuration(1, p, processor("Factory processor 2", "two"))
        for {
          _           <- changes.changes.offer(Add(cfg))
          _           <- signal.started.take.repeatN(1)
          before      <- signal.active.get
          _           <- changes.changes.offer(Update(cfg.copy(processors = NonEmptySet.one(p))))
          _           <- signal.started.take
          after       <- signal.active.get
          renamed      = cfg.copy(
                           name = "renamed",
                           processors = NonEmptySet.one(p.copy(graphId = "replacement"))
                         )
          _           <- changes.changes.offer(Update(renamed))
          _           <- signal.started.take
          afterRename <- signal.active.get
          _           <- changes.changes.offer(Delete(renamed))
          _           <- changes.changes.offer(Add(configuration(2, p)))
          _           <- signal.started.take
          afterDelete <- signal.active.get
        } yield assertTrue(before == 2, after == 1, afterRename == 1, afterDelete == 1)
      }
    },
    test("unknown and duplicate channel names are rejected on create and update") {
      ZIO.scoped {
        (for {
          units           <- ZIO.service[ProcessingUnitsRepositoryFake]
          manager         <- ZIO.service[FlowConfigurationManager]
          updates         <- ZIO.service[FlowConfigurationChanges]
          _               <- updates.changes.runDrain.forkScoped
          pt              <- ZIO.serviceWithZIO[PeripheryTypeRepository](
                               _.create(
                                 PeripheryType.New(
                                   "port",
                                   "",
                                   "",
                                   NonEmptyChunk(
                                     PeripheryType.Connection("channel", Direction.Both, "units", "Int", None)
                                   )
                                 )
                               )
                             )
          ct              <- ZIO.serviceWithZIO[ControllerTypeRepository](
                               _.create(
                                 ControllerType.New(
                                   "board",
                                   "",
                                   None,
                                   Map("port".toPeripheryName -> pt.id),
                                   None
                                 )
                               )
                             )
          controller      <- ZIO.serviceWithZIO[ControllerRepository](
                               _.create(Controller.New(ct.id, "board", "", None))
                             )
          _               <- units.create(
                               FlowConfigurationManagerSpec.withDefinition(
                                 ProcessorDefinition(
                                   "unit",
                                   "",
                                   Json.Obj(),
                                   Chunk(
                                     ProcessorDefinition.InputConnection("input1", "", "units", "Int"),
                                     ProcessorDefinition.InputConnection("input2", "", "units", "Int")
                                   ),
                                   Chunk(
                                     ProcessorDefinition.OutputConnection("output1", "", "units", "Int"),
                                     ProcessorDefinition.OutputConnection("output2", "", "units", "Int")
                                   )
                                 )
                               )
                             )
          p                = processor("unit", "one").copy(
                               inbound = Chunk(
                                 Address(controller.id, "port", "channel", "input1"),
                                 Address(controller.id, "port", "channel", "input2")
                               ),
                               outbound = Chunk(
                                 Address(controller.id, "port", "channel", "output1"),
                                 Address(controller.id, "port", "channel", "output2")
                               )
                             )
          good            <-
            manager.create(FlowConfiguration.New("good", "", Json.Obj(), NonEmptySet.one(p), None))
          invalid          = Chunk(
                               p.copy(inbound = p.inbound.map(_.copy(processorConnectionName = "unknown"))),
                               p.copy(outbound =
                                 p.outbound.map(_.copy(processorConnectionName = "unknown"))
                               ),
                               p.copy(inbound = Chunk.fill(2)(p.inbound.head)),
                               p.copy(outbound = Chunk.fill(2)(p.outbound.head))
                             )
          results         <-
            ZIO.foreach(invalid) { bad =>
              for {
                created <-
                  manager
                    .create(
                      FlowConfiguration.New("bad", "", Json.Obj(), NonEmptySet.one(bad), None)
                    )
                    .exit
                updated <- manager.update(good.copy(processors = NonEmptySet.one(bad))).exit
              } yield (created, updated)
            }
          stored          <- manager.get(good.id)
          saved           <- manager.list()
          reordered        =
            good.copy(processors =
              NonEmptySet.one(p.copy(inbound = p.inbound.reverse, outbound = p.outbound.reverse))
            )
          reorderedResult <- manager.update(reordered)
        } yield assertTrue(
          results.forall { case (created, updated) => created.isFailure && updated.isFailure },
          stored.contains(good),
          saved == Chunk(good),
          reorderedResult.contains(reordered)
        )).provideSome[Scope](
          FlowConfigurationManager.live,
          ProcessingUnitsRepositoryFake.empty,
          ControllerRepositoryFake.empty,
          ControllerTypeRepositoryFake.empty,
          PeripheryTypeRepositoryFake.empty,
          ConfigurationRepositoryFake.empty,
          FlowConfigurationChanges.live
        )
      }
    }
  )
}
