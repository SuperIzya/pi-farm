package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.*
import org.pi.farm.model.{*, given}
import org.pi.farm.plugin.DataProcessor
import org.pi.farm.plugin.DataProcessor.{noParamsCodec, NoParams}
import org.pi.farm.plugin.syntax.ConfigurableFlow
import org.pi.farm.storage.*

import zio.*
import zio.json.JsonCodec
import zio.json.ast.Json
import zio.test.*

import scala.language.implicitConversions

import cats.data.NonEmptySet

object ConfigurationManagerSpec extends PiFarmSpec {

  // ---- Helpers ----

  private type SetupEnv =
    ProcessingUnitsRepositoryFake & ControllerRepository & ControllerTypeRepository & PeripheryTypeRepository

  private case class Scenario(
    config: FlowConfiguration.New,
    pu: ProcessorDefinition
  )

  /** Builds a fully valid wiring scenario:
    *   - a processing unit named `puName` with 1 inbound (In / "degC" / Float) and 1 outbound (Out / "bool" / Boolean)
    *     channel
    *   - matching periphery types, controller types, and controllers
    *
    * Returns a `Configuration.New` that should pass all `ConfigurationManager` validation.
    */
  private def buildValid(puName: String = "TestUnit"): ZIO[SetupEnv, Throwable, Scenario] =
    for {
      puRepo <- ZIO.service[ProcessingUnitsRepositoryFake]
      ctRepo <- ZIO.service[ControllerTypeRepository]
      cRepo  <- ZIO.service[ControllerRepository]
      ptRepo <- ZIO.service[PeripheryTypeRepository]

      inboundPt  <- ptRepo.create(
                      PeripheryType.New(
                        name = "TempSensor",
                        description = "temperature sensor",
                        image = "img.png",
                        connections = NonEmptyChunk(
                          PeripheryType.Connection(
                            name = "in1",
                            direction = Direction.In,
                            units = "degC",
                            `type` = "Float"
                          )
                        )
                      )
                    )
      outboundPt <- ptRepo.create(
                      PeripheryType.New(
                        name = "Relay",
                        description = "relay",
                        image = "img.png",
                        connections = NonEmptyChunk(
                          PeripheryType.Connection(
                            name = "out1",
                            direction = Direction.Out,
                            units = "bool",
                            `type` = "Boolean"
                          )
                        )
                      )
                    )

      ctIn  <- ctRepo.create(
                 ControllerType.New(
                   name = "CTIn",
                   description = "d",
                   schema = None,
                   code = "",
                   peripheries = Map("p1".toPeripheryId -> inboundPt.id)
                 )
               )
      ctOut <- ctRepo.create(
                 ControllerType.New(
                   name = "CTOut",
                   description = "d",
                   schema = None,
                   code = "",
                   peripheries = Map("p1".toPeripheryId -> outboundPt.id)
                 )
               )

      cIn  <- cRepo.create(Controller.New(typeId = ctIn.id, name = "CIn", description = "d"))
      cOut <- cRepo.create(Controller.New(typeId = ctOut.id, name = "COut", description = "d"))

      pu = ProcessorDefinition(
             name = puName,
             description = "test unit",
             paramsSchema = Json.Obj(),
             inbound = Chunk(
               ProcessorDefinition.InputConnection(units = "degC", `type` = "Float", name = "in1", description = "")
             ),
             outbound = Chunk(
               ProcessorDefinition.OutputConnection(units = "bool", `type` = "Boolean", name = "out1", description = "")
             )
           )
      _ <- puRepo.create(withDefinition(pu))
    } yield Scenario(
      config = FlowConfiguration.New(
        name = puName,
        description = "test",
        processors = NonEmptySet.one(
          FlowConfiguration.Processor(
            unit = puName,
            parameters = Json.Obj(),
            inbound = Chunk(Address(cIn.id, "p1", "in1")),
            outbound = Chunk(Address(cOut.id, "p1", "out1"))
          )
        )
      ),
      pu = pu
    )

  /** A `Configuration.New` with a single processor with empty inbound/outbound, useful for bypassing validation when
    * inserting directly through the repository.
    */
  private def emptyConfigNew(puName: String): FlowConfiguration.New =
    FlowConfiguration.New(
      name = puName,
      description = "d",
      processors = NonEmptySet.one(FlowConfiguration.Processor(puName, Json.Obj(), Chunk.empty, Chunk.empty))
    )

  def withDefinition(definition: ProcessorDefinition): DataProcessor =
    new DataProcessor {
      type ParamsType = NoParams
      given paramsCodec: JsonCodec[ParamsType] = noParamsCodec

      override def work: ConfigurableFlow          = ???
      def processorDefinition: ProcessorDefinition = definition
    }
  // ---- Spec ----

  def spec = suite("ConfigurationManager")(
    suite("get, list, and delete delegate to the repository without validation")(
      test("list reflects configurations added directly to the repository") {
        for {
          repo    <- ZIO.service[ConfigurationRepository]
          manager <- ZIO.service[ConfigurationManager]
          before  <- manager.list().map(_.size)
          _       <- repo.create(emptyConfigNew("ListA"))
          _       <- repo.create(emptyConfigNew("ListB"))
          after   <- manager.list()
        } yield assertTrue(after.size == before + 2)
      },
      test("get returns Some for an existing configuration") {
        for {
          repo    <- ZIO.service[ConfigurationRepository]
          manager <- ZIO.service[ConfigurationManager]
          created <- repo.create(emptyConfigNew("GetExisting"))
          found   <- manager.get(created.id)
        } yield assertTrue(found.contains(created))
      },
      test("get returns None for a nonexistent id") {
        for {
          manager <- ZIO.service[ConfigurationManager]
          found   <- manager.get(99998)
        } yield assertTrue(found.isEmpty)
      },
      test("delete removes the configuration from the repository") {
        for {
          repo    <- ZIO.service[ConfigurationRepository]
          manager <- ZIO.service[ConfigurationManager]
          created <- repo.create(emptyConfigNew("DeleteMe"))
          _       <- manager.delete(created.id)
          found   <- manager.get(created.id)
        } yield assertTrue(found.isEmpty)
      }
    ),
    suite("create")(
      test("succeeds when all connections are valid") {
        for {
          scenario <- buildValid("CreateHappy")
          manager  <- ZIO.service[ConfigurationManager]
          created  <- manager.create(scenario.config)
        } yield {
          val p        = created.processors.head
          val expected = scenario.config.processors.head
          assertTrue(
            p.unit == expected.unit,
            p.inbound == expected.inbound,
            p.outbound == expected.outbound
          )
        }
      },
      test("fails when the processing unit is not found") {
        for {
          scenario <- buildValid("CreateKnown")
          manager  <- ZIO.service[ConfigurationManager]
          badConfig = scenario
                        .config
                        .copy(
                          processors = NonEmptySet.one(scenario.config.processors.head.copy(unit = "NonExistentUnit"))
                        )
          result   <- manager.create(badConfig).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when inbound address count does not match the processing unit's channel count") {
        for {
          scenario <- buildValid("CreateInboundMismatch")
          manager  <- ZIO.service[ConfigurationManager]
          // provide two inbound addresses where one is expected
          p         = scenario.config.processors.head
          doubled   = p.inbound ++ p.inbound
          badConfig = scenario
                        .config
                        .copy(
                          processors = NonEmptySet.one(p.copy(inbound = doubled))
                        )
          result   <- manager.create(badConfig).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when outbound address count does not match the processing unit's channel count") {
        for {
          scenario <- buildValid("CreateOutboundMismatch")
          manager  <- ZIO.service[ConfigurationManager]
          p         = scenario.config.processors.head
          badConfig = scenario
                        .config
                        .copy(
                          processors = NonEmptySet.one(p.copy(outbound = Chunk.empty))
                        )
          result   <- manager.create(badConfig).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the referenced controller does not exist") {
        for {
          scenario <- buildValid("CreateBadController")
          manager  <- ZIO.service[ConfigurationManager]
          p         = scenario.config.processors.head
          badAddr   = Chunk(Address(99999, "p1", "in1"))
          badConfig = scenario
                        .config
                        .copy(
                          processors = NonEmptySet.one(p.copy(inbound = badAddr))
                        )
          result   <- manager.create(badConfig).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the controller's type does not exist") {
        for {
          puRepo  <- ZIO.service[ProcessingUnitsRepositoryFake]
          cRepo   <- ZIO.service[ControllerRepository]
          manager <- ZIO.service[ConfigurationManager]
          // create a controller whose typeId points to a nonexistent controller type
          orphan  <- cRepo.create(Controller.New(typeId = 99999, name = "Orphan", description = "d"))
          _       <- puRepo.create(
                       withDefinition(
                         ProcessorDefinition(
                           name = "OrphanUnit",
                           description = "d",
                           paramsSchema = Json.Obj(),
                           inbound = Chunk(ProcessorDefinition.InputConnection("in1", "", "degC", "Float")),
                           outbound = Chunk.empty
                         )
                       )
                     )
          config   = FlowConfiguration.New(
                       name = "cfg",
                       description = "d",
                       processors = NonEmptySet.one(
                         FlowConfiguration.Processor(
                           "OrphanUnit",
                           Json.Obj(),
                           Chunk(Address(orphan.id, "p1", "in1")),
                           Chunk.empty
                         )
                       )
                     )
          result  <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the periphery id is not registered on the controller type") {
        for {
          scenario <- buildValid("CreateBadPeriphery")
          manager  <- ZIO.service[ConfigurationManager]
          p         = scenario.config.processors.head
          // "p999" is not in the controller type's peripheries map
          badAddr   = Chunk(Address(p.inbound.head.controllerId, "p999", "in1"))
          badConfig = scenario
                        .config
                        .copy(
                          processors = NonEmptySet.one(p.copy(inbound = badAddr))
                        )
          result   <- manager.create(badConfig).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the periphery type id in the controller type does not exist") {
        for {
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepositoryFake]
          manager <- ZIO.service[ConfigurationManager]
          // create a controller type whose periphery type id doesn't exist
          ct      <- ctRepo.create(
                       ControllerType.New(
                         name = "GhostPT",
                         description = "d",
                         schema = None,
                         code = "",
                         peripheries = Map("p1".toPeripheryId -> (99999: PeripheryTypeId))
                       )
                     )
          c       <- cRepo.create(Controller.New(typeId = ct.id, name = "GhostCtrl", description = "d"))
          _       <- puRepo.create(
                       withDefinition(
                         ProcessorDefinition(
                           name = "GhostUnit",
                           description = "d",
                           paramsSchema = Json.Obj(),
                           inbound = Chunk(ProcessorDefinition.InputConnection("in1", "", "degC", "Float")),
                           outbound = Chunk.empty
                         )
                       )
                     )
          config   = FlowConfiguration.New(
                       name = "cfg",
                       description = "d",
                       processors = NonEmptySet.one(
                         FlowConfiguration.Processor(
                           "GhostUnit",
                           Json.Obj(),
                           Chunk(Address(c.id, "p1", "in1")),
                           Chunk.empty
                         )
                       )
                     )
          result  <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the periphery direction does not match the channel direction") {
        for {
          ptRepo  <- ZIO.service[PeripheryTypeRepository]
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepositoryFake]
          manager <- ZIO.service[ConfigurationManager]
          // Direction.Out periphery on an inbound (Direction.In) channel
          pt      <- ptRepo.create(
                       PeripheryType.New(
                         name = "WrongDir",
                         description = "d",
                         image = "img.png",
                         connections = NonEmptyChunk(
                           PeripheryType.Connection(
                             name = "in1",
                             direction = Direction.Out,
                             units = "degC",
                             `type` = "Float"
                           )
                         )
                       )
                     )
          ct      <- ctRepo.create(
                       ControllerType.New(
                         name = "DirCT",
                         description = "d",
                         schema = None,
                         code = "",
                         peripheries = Map("p1".toPeripheryId -> pt.id)
                       )
                     )
          c       <- cRepo.create(Controller.New(typeId = ct.id, name = "DirCtrl", description = "d"))
          _       <- puRepo.create(
                       withDefinition(
                         ProcessorDefinition(
                           name = "DirUnit",
                           description = "d",
                           paramsSchema = Json.Obj(),
                           inbound = Chunk(ProcessorDefinition.InputConnection("in1", "", "degC", "Float")),
                           outbound = Chunk.empty
                         )
                       )
                     )
          config   = FlowConfiguration.New(
                       name = "cfg",
                       description = "d",
                       processors = NonEmptySet.one(
                         FlowConfiguration.Processor(
                           "DirUnit",
                           Json.Obj(),
                           Chunk(Address(c.id, "p1", "in1")),
                           Chunk.empty
                         )
                       )
                     )
          result  <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("Direction.Both periphery is accepted for any channel direction") {
        for {
          ptRepo  <- ZIO.service[PeripheryTypeRepository]
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepositoryFake]
          manager <- ZIO.service[ConfigurationManager]
          pt      <- ptRepo.create(
                       PeripheryType.New(
                         name = "BothDir",
                         description = "d",
                         image = "img.png",
                         connections = NonEmptyChunk(
                           PeripheryType.Connection(
                             name = "in1",
                             direction = Direction.Both,
                             units = "degC",
                             `type` = "Float"
                           )
                         )
                       )
                     )
          ct      <- ctRepo.create(
                       ControllerType.New(
                         name = "BothCT",
                         description = "d",
                         schema = None,
                         code = "",
                         peripheries = Map("p1".toPeripheryId -> pt.id)
                       )
                     )
          c       <- cRepo.create(Controller.New(typeId = ct.id, name = "BothCtrl", description = "d"))
          _       <- puRepo.create(
                       withDefinition(
                         ProcessorDefinition(
                           name = "BothUnit",
                           description = "d",
                           paramsSchema = Json.Obj(),
                           inbound = Chunk(ProcessorDefinition.InputConnection("in1", "", "degC", "Float")),
                           outbound = Chunk.empty
                         )
                       )
                     )
          config   = FlowConfiguration.New(
                       name = "cfg",
                       description = "d",
                       processors = NonEmptySet.one(
                         FlowConfiguration.Processor(
                           "BothUnit",
                           Json.Obj(),
                           Chunk(Address(c.id, "p1", "in1")),
                           Chunk.empty
                         )
                       )
                     )
          created <- manager.create(config)
        } yield assertTrue(created.processors.head.inbound.size == 1)
      },
      test("fails when the periphery units do not match the channel units") {
        for {
          ptRepo  <- ZIO.service[PeripheryTypeRepository]
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepositoryFake]
          manager <- ZIO.service[ConfigurationManager]
          // periphery has "degF", channel expects "degC"
          pt      <- ptRepo.create(
                       PeripheryType.New(
                         name = "WrongUnits",
                         description = "d",
                         image = "img.png",
                         connections = NonEmptyChunk(
                           PeripheryType.Connection(
                             name = "in1",
                             direction = Direction.In,
                             units = "degF",
                             `type` = "Float"
                           )
                         )
                       )
                     )
          ct      <- ctRepo.create(
                       ControllerType.New(
                         name = "UnitsCT",
                         description = "d",
                         schema = None,
                         code = "",
                         peripheries = Map("p1".toPeripheryId -> pt.id)
                       )
                     )
          c       <- cRepo.create(Controller.New(typeId = ct.id, name = "UnitsCtrl", description = "d"))
          _       <- puRepo.create(
                       withDefinition(
                         ProcessorDefinition(
                           name = "UnitsUnit",
                           description = "d",
                           paramsSchema = Json.Obj(),
                           inbound = Chunk(ProcessorDefinition.InputConnection("in1", "", "degC", "Float")),
                           outbound = Chunk.empty
                         )
                       )
                     )
          config   = FlowConfiguration.New(
                       name = "cfg",
                       description = "d",
                       processors = NonEmptySet.one(
                         FlowConfiguration.Processor(
                           "UnitsUnit",
                           Json.Obj(),
                           Chunk(Address(c.id, "p1", "in1")),
                           Chunk.empty
                         )
                       )
                     )
          result  <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the periphery type does not match the channel type") {
        for {
          ptRepo  <- ZIO.service[PeripheryTypeRepository]
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepositoryFake]
          manager <- ZIO.service[ConfigurationManager]
          // periphery has "Boolean", channel expects "Float"
          pt      <- ptRepo.create(
                       PeripheryType.New(
                         name = "WrongType",
                         description = "d",
                         image = "img.png",
                         connections = NonEmptyChunk(
                           PeripheryType.Connection(
                             name = "in1",
                             direction = Direction.In,
                             units = "degC",
                             `type` = "Boolean"
                           )
                         )
                       )
                     )
          ct      <- ctRepo.create(
                       ControllerType.New(
                         name = "TypeCT",
                         description = "d",
                         schema = None,
                         code = "",
                         peripheries = Map("p1".toPeripheryId -> pt.id)
                       )
                     )
          c       <- cRepo.create(Controller.New(typeId = ct.id, name = "TypeCtrl", description = "d"))
          _       <- puRepo.create(
                       withDefinition(
                         ProcessorDefinition(
                           name = "TypeUnit",
                           description = "d",
                           paramsSchema = Json.Obj(),
                           inbound = Chunk(ProcessorDefinition.InputConnection("in1", "", "degC", "Float")),
                           outbound = Chunk.empty
                         )
                       )
                     )
          config   = FlowConfiguration.New(
                       name = "cfg",
                       description = "d",
                       processors = NonEmptySet.one(
                         FlowConfiguration.Processor(
                           "TypeUnit",
                           Json.Obj(),
                           Chunk(Address(c.id, "p1", "in1")),
                           Chunk.empty
                         )
                       )
                     )
          result  <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("update")(
      test("succeeds when all connections are valid") {
        for {
          scenario <- buildValid("UpdateHappy")
          repo     <- ZIO.service[ConfigurationRepository]
          manager  <- ZIO.service[ConfigurationManager]
          created  <- repo.create(emptyConfigNew("UpdateBase"))
          sp        = scenario.config.processors.head
          result   <- manager.update(
                        created.copy(
                          processors = NonEmptySet.one(
                            FlowConfiguration.Processor(sp.unit, sp.parameters, sp.inbound, sp.outbound)
                          )
                        )
                      )
        } yield assertTrue(result.isDefined)
      },
      test("fails when the processing unit is not found during update") {
        for {
          repo    <- ZIO.service[ConfigurationRepository]
          manager <- ZIO.service[ConfigurationManager]
          created <- repo.create(emptyConfigNew("UpdateNoUnit"))
          result  <- manager
                       .update(
                         created.copy(
                           processors = NonEmptySet.one(
                             FlowConfiguration.Processor("AbsolutelyMissingUnit", Json.Obj(), Chunk.empty, Chunk.empty)
                           )
                         )
                       )
                       .exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("multi-processor configurations")(
      test("create succeeds with multiple valid processors") {
        for {
          scenarioA <- buildValid("MultiA")
          scenarioB <- buildValid("MultiB")
          manager   <- ZIO.service[ConfigurationManager]
          pA         = scenarioA.config.processors.head
          pB         = scenarioB.config.processors.head
          config     = FlowConfiguration.New(
                         name = "multi",
                         description = "multi-processor config",
                         processors = NonEmptySet.of(pA, pB)
                       )
          created   <- manager.create(config)
        } yield assertTrue(
          created.processors.length == 2,
          created.processors.exists(_.unit == "MultiA"),
          created.processors.exists(_.unit == "MultiB")
        )
      },
      test("create fails when one of multiple processors references non-existent unit") {
        for {
          scenario <- buildValid("MultiValid")
          manager  <- ZIO.service[ConfigurationManager]
          validP    = scenario.config.processors.head
          invalidP  = FlowConfiguration.Processor("NonExistentUnit", Json.Obj(), Chunk.empty, Chunk.empty)
          config    = FlowConfiguration.New(
                        name = "multi-bad",
                        description = "d",
                        processors = NonEmptySet.of(validP, invalidP)
                      )
          result   <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("create fails when one of multiple processors has mismatched inbound count") {
        for {
          scenarioA <- buildValid("MultiInA")
          scenarioB <- buildValid("MultiInB")
          manager   <- ZIO.service[ConfigurationManager]
          pA         = scenarioA.config.processors.head
          pB         = scenarioB.config.processors.head
          // Double inbound for processor B — mismatch
          badPB      = pB.copy(inbound = pB.inbound ++ pB.inbound)
          config     = FlowConfiguration.New(
                         name = "multi-mismatch",
                         description = "d",
                         processors = NonEmptySet.of(pA, badPB)
                       )
          result    <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("create succeeds when processors share the same controller") {
        for {
          puRepo <- ZIO.service[ProcessingUnitsRepositoryFake]
          ctRepo <- ZIO.service[ControllerTypeRepository]
          cRepo  <- ZIO.service[ControllerRepository]
          ptRepo <- ZIO.service[PeripheryTypeRepository]

          pt     <- ptRepo.create(
                      PeripheryType.New(
                        name = "SharedSensor",
                        description = "d",
                        image = "img.png",
                        connections = NonEmptyChunk(
                          PeripheryType.Connection(name = "in1", direction = Direction.In, units = "degC", `type` = "Float")
                        )
                      )
                    )
          ct     <- ctRepo.create(
                      ControllerType.New(
                        name = "SharedCT",
                        description = "d",
                        schema = None,
                        code = "",
                        peripheries = Map("p1".toPeripheryId -> pt.id)
                      )
                    )
          shared <- cRepo.create(Controller.New(typeId = ct.id, name = "SharedCtrl", description = "d"))

          _ <- puRepo.create(
                 withDefinition(
                   ProcessorDefinition(
                     name = "SharedUnitA",
                     description = "d",
                     paramsSchema = Json.Obj(),
                     inbound = Chunk(ProcessorDefinition.InputConnection("in1", "", "degC", "Float")),
                     outbound = Chunk.empty
                   )
                 )
               )
          _ <- puRepo.create(
                 withDefinition(
                   ProcessorDefinition(
                     name = "SharedUnitB",
                     description = "d",
                     paramsSchema = Json.Obj(),
                     inbound = Chunk(ProcessorDefinition.InputConnection("in1", "", "degC", "Float")),
                     outbound = Chunk.empty
                   )
                 )
               )

          manager <- ZIO.service[ConfigurationManager]
          pA       =
            FlowConfiguration.Processor("SharedUnitA", Json.Obj(), Chunk(Address(shared.id, "p1", "in1")), Chunk.empty)
          pB       =
            FlowConfiguration.Processor("SharedUnitB", Json.Obj(), Chunk(Address(shared.id, "p1", "in1")), Chunk.empty)
          config   = FlowConfiguration.New(
                       name = "shared-ctrl",
                       description = "d",
                       processors = NonEmptySet.of(pA, pB)
                     )
          created <- manager.create(config)
        } yield assertTrue(
          created.processors.length == 2,
          created.processors.forall(_.inbound.head.controllerId == shared.id)
        )
      },
      test("update to multiple processors succeeds") {
        for {
          scenarioA <- buildValid("UpdateMultiA")
          scenarioB <- buildValid("UpdateMultiB")
          repo      <- ZIO.service[ConfigurationRepository]
          manager   <- ZIO.service[ConfigurationManager]
          created   <- repo.create(emptyConfigNew("UpdateMultiBase"))
          pA         = scenarioA.config.processors.head
          pB         = scenarioB.config.processors.head
          result    <- manager.update(
                         created.copy(
                           processors = NonEmptySet.of(
                             FlowConfiguration.Processor(pA.unit, pA.parameters, pA.inbound, pA.outbound),
                             FlowConfiguration.Processor(pB.unit, pB.parameters, pB.inbound, pB.outbound)
                           )
                         )
                       )
        } yield assertTrue(
          result.isDefined,
          result.get.processors.length == 2
        )
      },
      test("get roundtrip preserves all processors") {
        for {
          scenarioA <- buildValid("RoundtripA")
          scenarioB <- buildValid("RoundtripB")
          manager   <- ZIO.service[ConfigurationManager]
          pA         = scenarioA.config.processors.head
          pB         = scenarioB.config.processors.head
          config     = FlowConfiguration.New(
                         name = "roundtrip",
                         description = "d",
                         processors = NonEmptySet.of(pA, pB)
                       )
          created   <- manager.create(config)
          retrieved <- manager.get(created.id)
        } yield assertTrue(
          retrieved.isDefined,
          retrieved.get.processors.length == 2,
          retrieved.get.processors == created.processors
        )
      }
    )
  ).provide(
    ConfigurationManager.live,
    ProcessingUnitsRepositoryFake.empty,
    ControllerRepositoryFake.empty,
    ControllerTypeRepositoryFake.empty,
    PeripheryTypeRepositoryFake.empty,
    ConfigurationRepositoryFake.empty
  ) @@ TestAspect.sequential
}
