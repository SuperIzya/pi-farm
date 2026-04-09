package org.pi.farm.service

import org.pi.farm.fake.*
import org.pi.farm.model.{*, given}
import org.pi.farm.storage.*
import zio.*
import zio.json.ast.Json
import zio.test.*

import scala.language.implicitConversions

object ConfigurationManagerSpec extends ZIOSpecDefault {

  override def aspects: Chunk[TestAspectAtLeastR[zio.test.TestEnvironment]] =
    Chunk(TestAspect.sequential)

  // ---- Helpers ----

  private type SetupEnv =
    ProcessingUnitsRepository & ControllerRepository & ControllerTypeRepository & PeripheryTypeRepository

  private case class Scenario(
    config: Configuration.New,
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
      puRepo <- ZIO.service[ProcessingUnitsRepository]
      ctRepo <- ZIO.service[ControllerTypeRepository]
      cRepo  <- ZIO.service[ControllerRepository]
      ptRepo <- ZIO.service[PeripheryTypeRepository]

      inboundPt <- ptRepo.create(
        PeripheryType.New(
          name = "TempSensor",
          units = "degC",
          `type` = "Float",
          description = "temperature sensor",
          image = "img.png",
          direction = Direction.In
        )
      )
      outboundPt <- ptRepo.create(
        PeripheryType.New(
          name = "Relay",
          units = "bool",
          `type` = "Boolean",
          description = "relay",
          image = "img.png",
          direction = Direction.Out
        )
      )

      ctIn <- ctRepo.create(
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
      _ <- puRepo.create(pu)
    } yield Scenario(
      config = Configuration.New(
        name = puName,
        description = "test",
        inbound = Chunk(Address(cIn.id, "p1", "in1")),
        outbound = Chunk(Address(cOut.id, "p1", "out1")),
        processingUnit = puName,
        additional = Json.Obj()
      ),
      pu = pu
    )

  /** A `Configuration.New` with empty inbound/outbound, useful for bypassing validation when inserting directly through
    * the repository.
    */
  private def emptyConfigNew(puName: String): Configuration.New =
    Configuration.New(
      name = puName,
      description = "d",
      inbound = Chunk.empty,
      outbound = Chunk.empty,
      processingUnit = puName,
      additional = Json.Obj()
    )

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
        } yield assertTrue(
          created.processingUnit == scenario.config.processingUnit,
          created.inbound == scenario.config.inbound,
          created.outbound == scenario.config.outbound
        )
      },
      test("fails when the processing unit is not found") {
        for {
          scenario <- buildValid("CreateKnown")
          manager  <- ZIO.service[ConfigurationManager]
          result   <- manager.create(scenario.config.copy(processingUnit = "NonExistentUnit")).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when inbound address count does not match the processing unit's channel count") {
        for {
          scenario <- buildValid("CreateInboundMismatch")
          manager  <- ZIO.service[ConfigurationManager]
          // provide two inbound addresses where one is expected
          doubled = scenario.config.inbound ++ scenario.config.inbound
          result <- manager.create(scenario.config.copy(inbound = doubled)).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when outbound address count does not match the processing unit's channel count") {
        for {
          scenario <- buildValid("CreateOutboundMismatch")
          manager  <- ZIO.service[ConfigurationManager]
          result   <- manager.create(scenario.config.copy(outbound = Chunk.empty)).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the referenced controller does not exist") {
        for {
          scenario <- buildValid("CreateBadController")
          manager  <- ZIO.service[ConfigurationManager]
          badAddr = Chunk(Address(99999, "p1", "in1"))
          result <- manager.create(scenario.config.copy(inbound = badAddr)).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the controller's type does not exist") {
        for {
          puRepo  <- ZIO.service[ProcessingUnitsRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          manager <- ZIO.service[ConfigurationManager]
          // create a controller whose typeId points to a nonexistent controller type
          orphan <- cRepo.create(Controller.New(typeId = 99999, name = "Orphan", description = "d"))
          _      <- puRepo.create(
            ProcessorDefinition(
              name = "OrphanUnit",
              description = "d",
              paramsSchema = Json.Obj(),
              inbound = Chunk(ProcessorDefinition.InputConnection("degC", "", "Float", "in1")),
              outbound = Chunk.empty
            )
          )
          config = Configuration.New(
            name = "cfg",
            description = "d",
            inbound = Chunk(Address(orphan.id, "p1", "in1")),
            outbound = Chunk.empty,
            processingUnit = "OrphanUnit",
            additional = Json.Obj()
          )
          result <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the periphery id is not registered on the controller type") {
        for {
          scenario <- buildValid("CreateBadPeriphery")
          manager  <- ZIO.service[ConfigurationManager]
          // "p999" is not in the controller type's peripheries map
          badAddr = Chunk(Address(scenario.config.inbound.head.controllerId, "p999", "in1"))
          result <- manager.create(scenario.config.copy(inbound = badAddr)).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the periphery type id in the controller type does not exist") {
        for {
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepository]
          manager <- ZIO.service[ConfigurationManager]
          // create a controller type whose periphery type id doesn't exist
          ct <- ctRepo.create(
            ControllerType.New(
              name = "GhostPT",
              description = "d",
              schema = None,
              code = "",
              peripheries = Map("p1".toPeripheryId -> (99999: PeripheryTypeId))
            )
          )
          c <- cRepo.create(Controller.New(typeId = ct.id, name = "GhostCtrl", description = "d"))
          _ <- puRepo.create(
            ProcessorDefinition(
              name = "GhostUnit",
              description = "d",
              paramsSchema = Json.Obj(),
              inbound = Chunk(ProcessorDefinition.InputConnection("degC", "", "Float", "in1")),
              outbound = Chunk.empty
            )
          )
          config = Configuration.New(
            name = "cfg",
            description = "d",
            inbound = Chunk(Address(c.id, "p1", "in1")),
            outbound = Chunk.empty,
            processingUnit = "GhostUnit",
            additional = Json.Obj()
          )
          result <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the periphery direction does not match the channel direction") {
        for {
          ptRepo  <- ZIO.service[PeripheryTypeRepository]
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepository]
          manager <- ZIO.service[ConfigurationManager]
          // Direction.Out periphery on an inbound (Direction.In) channel
          pt <- ptRepo.create(
            PeripheryType.New(
              name = "WrongDir",
              units = "degC",
              `type` = "Float",
              description = "d",
              image = "img.png",
              direction = Direction.Out
            )
          )
          ct <- ctRepo.create(
            ControllerType.New(
              name = "DirCT",
              description = "d",
              schema = None,
              code = "",
              peripheries = Map("p1".toPeripheryId -> pt.id)
            )
          )
          c <- cRepo.create(Controller.New(typeId = ct.id, name = "DirCtrl", description = "d"))
          _ <- puRepo.create(
            ProcessorDefinition(
              name = "DirUnit",
              description = "d",
              paramsSchema = Json.Obj(),
              inbound = Chunk(ProcessorDefinition.InputConnection("degC", "", "Float", "in1")),
              outbound = Chunk.empty
            )
          )
          config = Configuration.New(
            name = "cfg",
            description = "d",
            inbound = Chunk(Address(c.id, "p1", "in1")),
            outbound = Chunk.empty,
            processingUnit = "DirUnit",
            additional = Json.Obj()
          )
          result <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("Direction.Both periphery is accepted for any channel direction") {
        for {
          ptRepo  <- ZIO.service[PeripheryTypeRepository]
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepository]
          manager <- ZIO.service[ConfigurationManager]
          pt      <- ptRepo.create(
            PeripheryType.New(
              name = "BothDir",
              units = "degC",
              `type` = "Float",
              description = "d",
              image = "img.png",
              direction = Direction.Both
            )
          )
          ct <- ctRepo.create(
            ControllerType.New(
              name = "BothCT",
              description = "d",
              schema = None,
              code = "",
              peripheries = Map("p1".toPeripheryId -> pt.id)
            )
          )
          c <- cRepo.create(Controller.New(typeId = ct.id, name = "BothCtrl", description = "d"))
          _ <- puRepo.create(
            ProcessorDefinition(
              name = "BothUnit",
              description = "d",
              paramsSchema = Json.Obj(),
              inbound = Chunk(ProcessorDefinition.InputConnection("degC", "", "Float", "in1")),
              outbound = Chunk.empty
            )
          )
          config = Configuration.New(
            name = "cfg",
            description = "d",
            inbound = Chunk(Address(c.id, "p1", "in1")),
            outbound = Chunk.empty,
            processingUnit = "BothUnit",
            additional = Json.Obj()
          )
          created <- manager.create(config)
        } yield assertTrue(created.inbound.size == 1)
      },
      test("fails when the periphery units do not match the channel units") {
        for {
          ptRepo  <- ZIO.service[PeripheryTypeRepository]
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepository]
          manager <- ZIO.service[ConfigurationManager]
          // periphery has "degF", channel expects "degC"
          pt <- ptRepo.create(
            PeripheryType.New(
              name = "WrongUnits",
              units = "degF",
              `type` = "Float",
              description = "d",
              image = "img.png",
              direction = Direction.In
            )
          )
          ct <- ctRepo.create(
            ControllerType.New(
              name = "UnitsCT",
              description = "d",
              schema = None,
              code = "",
              peripheries = Map("p1".toPeripheryId -> pt.id)
            )
          )
          c <- cRepo.create(Controller.New(typeId = ct.id, name = "UnitsCtrl", description = "d"))
          _ <- puRepo.create(
            ProcessorDefinition(
              name = "UnitsUnit",
              description = "d",
              paramsSchema = Json.Obj(),
              inbound = Chunk(ProcessorDefinition.InputConnection("degC", "", "Float", "in1")),
              outbound = Chunk.empty
            )
          )
          config = Configuration.New(
            name = "cfg",
            description = "d",
            inbound = Chunk(Address(c.id, "p1", "in1")),
            outbound = Chunk.empty,
            processingUnit = "UnitsUnit",
            additional = Json.Obj()
          )
          result <- manager.create(config).exit
        } yield assertTrue(result.isFailure)
      },
      test("fails when the periphery type does not match the channel type") {
        for {
          ptRepo  <- ZIO.service[PeripheryTypeRepository]
          ctRepo  <- ZIO.service[ControllerTypeRepository]
          cRepo   <- ZIO.service[ControllerRepository]
          puRepo  <- ZIO.service[ProcessingUnitsRepository]
          manager <- ZIO.service[ConfigurationManager]
          // periphery has "Boolean", channel expects "Float"
          pt <- ptRepo.create(
            PeripheryType.New(
              name = "WrongType",
              units = "degC",
              `type` = "Boolean",
              description = "d",
              image = "img.png",
              direction = Direction.In
            )
          )
          ct <- ctRepo.create(
            ControllerType.New(
              name = "TypeCT",
              description = "d",
              schema = None,
              code = "",
              peripheries = Map("p1".toPeripheryId -> pt.id)
            )
          )
          c <- cRepo.create(Controller.New(typeId = ct.id, name = "TypeCtrl", description = "d"))
          _ <- puRepo.create(
            ProcessorDefinition(
              name = "TypeUnit",
              description = "d",
              paramsSchema = Json.Obj(),
              inbound = Chunk(ProcessorDefinition.InputConnection("degC", "", "Float", "in1")),
              outbound = Chunk.empty
            )
          )
          config = Configuration.New(
            name = "cfg",
            description = "d",
            inbound = Chunk(Address(c.id, "p1", "in1")),
            outbound = Chunk.empty,
            processingUnit = "TypeUnit",
            additional = Json.Obj()
          )
          result <- manager.create(config).exit
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
          result   <- manager.update(
            created.copy(
              processingUnit = scenario.config.processingUnit,
              inbound = scenario.config.inbound,
              outbound = scenario.config.outbound
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
                processingUnit = "AbsolutelyMissingUnit",
                inbound = Chunk.empty,
                outbound = Chunk.empty
              )
            )
            .exit
        } yield assertTrue(result.isFailure)
      }
    )
  ).provide(
    ConfigurationManager.live,
    ProcessingUnitsRepositoryFake.empty,
    ControllerRepositoryFake.empty,
    ControllerTypeRepositoryFake.empty,
    PeripheryTypeRepositoryFake.empty,
    ConfigurationRepositoryFake.empty
  )
}
