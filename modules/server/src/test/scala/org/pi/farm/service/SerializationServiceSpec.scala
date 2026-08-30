package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.*
import org.pi.farm.model.{Address, Controller, ControllerType, Direction, FlowConfiguration, PeripheryType}
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.storage.*

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*

import scala.collection.immutable.SortedSet
import scala.language.implicitConversions

import cats.data.NonEmptySet

object SerializationServiceSpec extends PiFarmSpec {

  private val layers =
    PeripheryTypeRepositoryFake.empty ++
      ControllerTypeRepositoryFake.empty ++
      ControllerRepositoryFake.empty ++
      ConfigurationRepositoryFake.empty >+>
      SerializationService.live

  private def mkPeripheryNew(name: String = "Sensor") =
    PeripheryType.New(
      name = name,
      description = "test periphery",
      image = "data:image/png;base64,abc",
      connections = NonEmptyChunk(
        PeripheryType.Connection(name = "ch1", direction = Direction.In, units = "degC", `type` = "Float")
      )
    )

  private def mkControllerTypeNew(peripheries: Map[PeripheryName, PeripheryTypeId]) =
    ControllerType.New(
      name = "TestCT",
      description = "test controller type",
      schema = None,
      code = "void setup() {}",
      peripheries = peripheries
    )

  private def mkControllerNew(typeId: ControllerTypeId) =
    Controller.New(typeId = typeId, name = "TestCtrl", description = "test controller")

  private def mkConfigNew(
    processors: NonEmptySet[FlowConfiguration.Processor]
  ) =
    FlowConfiguration.New(
      name = "TestConfig",
      description = "test configuration",
      graphData = Json.Obj(),
      processors = processors
    )

  // -- helpers to populate repos and produce entities --

  private def createPeripheryType(name: String = "Sensor") =
    ZIO.serviceWithZIO[PeripheryTypeRepository](_.create(mkPeripheryNew(name)))

  private def createControllerType(peripheries: Map[PeripheryName, PeripheryTypeId]) =
    ZIO.serviceWithZIO[ControllerTypeRepository](_.create(mkControllerTypeNew(peripheries)))

  private def createController(typeId: ControllerTypeId) =
    ZIO.serviceWithZIO[ControllerRepository](_.create(mkControllerNew(typeId)))

  private def createConfiguration(processors: NonEmptySet[FlowConfiguration.Processor]) =
    ZIO.serviceWithZIO[ConfigurationRepository](_.create(mkConfigNew(processors)))

  // -- Builds a full object graph: periphery -> controller type -> controller -> configuration --
  private def buildFullGraph = for {
    pt   <- createPeripheryType()
    ct   <- createControllerType(Map("p1".toPeripheryName -> pt.id))
    ctrl <- createController(ct.id)
    cfg  <- createConfiguration(
              NonEmptySet.one(
                FlowConfiguration.Processor(
                  unit = "TestUnit",
                  parameters = Json.Obj("key" -> Json.Str("value")),
                  inbound = Chunk(Address(ctrl.id, "p1", "ch1", "input1")),
                  outbound = Chunk.empty,
                  graphId = "graph1"
                )
              )
            )
  } yield (pt, ct, ctrl, cfg)

  def spec = suite("SerializationService")(
    suite("exportPeripheryType / importPeripheryType")(
      test("roundtrip preserves periphery type data") {
        for {
          svc      <- ZIO.service[SerializationService]
          original <- createPeripheryType()
          json     <- svc.exportPeripheryType(original.id)
          imported <- svc.importPeripheryType(json)
        } yield assertTrue(
          imported.name == original.name,
          imported.description == original.description,
          imported.image == original.image,
          imported.connections == original.connections,
          imported.id != original.id
        )
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[SerializationService]
          result <- svc.exportPeripheryType(99999).exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("exportControllerType / importControllerType")(
      test("roundtrip preserves controller type data") {
        for {
          svc      <- ZIO.service[SerializationService]
          pt       <- createPeripheryType()
          original <- createControllerType(Map("p1".toPeripheryName -> pt.id))
          json     <- svc.exportControllerType(original.id)
          imported <- svc.importControllerType(json)
        } yield assertTrue(
          imported.name == original.name,
          imported.description == original.description,
          imported.code == original.code,
          imported.schema == original.schema,
          imported.peripheries.size == original.peripheries.size,
          imported.id != original.id
        )
      },
      test("roundtrip preserves multiple peripheries") {
        for {
          svc      <- ZIO.service[SerializationService]
          pt1      <- createPeripheryType("SensorA")
          pt2      <- createPeripheryType("SensorB")
          original <- createControllerType(
                        Map("p1".toPeripheryName -> pt1.id, "p2".toPeripheryName -> pt2.id)
                      )
          json     <- svc.exportControllerType(original.id)
          imported <- svc.importControllerType(json)
        } yield assertTrue(
          imported.peripheries.size == 2,
          imported.peripheries.keySet == original.peripheries.keySet
        )
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[SerializationService]
          result <- svc.exportControllerType(99999).exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("exportController / importController")(
      test("roundtrip preserves controller data") {
        for {
          svc      <- ZIO.service[SerializationService]
          pt       <- createPeripheryType()
          ct       <- createControllerType(Map("p1".toPeripheryName -> pt.id))
          original <- createController(ct.id)
          json     <- svc.exportController(original.id)
          imported <- svc.importController(json)
        } yield assertTrue(
          imported.name == original.name,
          imported.description == original.description,
          imported.id != original.id
        )
      },
      test("imported controller gets new controller type and periphery type ids") {
        for {
          svc      <- ZIO.service[SerializationService]
          ptRepo   <- ZIO.service[PeripheryTypeRepository]
          ctRepo   <- ZIO.service[ControllerTypeRepository]
          pt       <- createPeripheryType()
          ct       <- createControllerType(Map("p1".toPeripheryName -> pt.id))
          original <- createController(ct.id)
          json     <- svc.exportController(original.id)
          imported <- svc.importController(json)
          // the imported controller should reference a new controller type
          newCt    <- ctRepo.get(imported.typeId)
        } yield assertTrue(
          imported.typeId != ct.id,
          newCt.isDefined,
          newCt.get.name == ct.name
        )
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[SerializationService]
          result <- svc.exportController(99999).exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("exportConfiguration / importConfiguration")(
      test("roundtrip preserves configuration data") {
        for {
          svc                    <- ZIO.service[SerializationService]
          (pt, ct, ctrl, config) <- buildFullGraph
          json                   <- svc.exportConfiguration(config.id)
          imported               <- svc.importConfiguration(json)
        } yield assertTrue(
          imported.name == config.name,
          imported.description == config.description,
          imported.processors.length == config.processors.length,
          imported.processors.head.unit == config.processors.head.unit,
          imported.processors.head.parameters == config.processors.head.parameters,
          imported.id != config.id
        )
      },
      test("imported configuration references new controller ids") {
        for {
          svc                    <- ZIO.service[SerializationService]
          (pt, ct, ctrl, config) <- buildFullGraph
          json                   <- svc.exportConfiguration(config.id)
          imported               <- svc.importConfiguration(json)
          originalCId             = config.processors.head.inbound.head.controllerId
          importedCId             = imported.processors.head.inbound.head.controllerId
        } yield assertTrue(importedCId != originalCId)
      },
      test("imported configuration creates new periphery types, controller types, and controllers") {
        for {
          svc                    <- ZIO.service[SerializationService]
          ptRepo                 <- ZIO.service[PeripheryTypeRepository]
          ctRepo                 <- ZIO.service[ControllerTypeRepository]
          cRepo                  <- ZIO.service[ControllerRepository]
          ptBefore               <- ptRepo.list().map(_.size)
          ctBefore               <- ctRepo.list().map(_.size)
          cBefore                <- cRepo.list().map(_.size)
          (pt, ct, ctrl, config) <- buildFullGraph
          json                   <- svc.exportConfiguration(config.id)
          _                      <- svc.importConfiguration(json)
          ptAfter                <- ptRepo.list().map(_.size)
          ctAfter                <- ctRepo.list().map(_.size)
          cAfter                 <- cRepo.list().map(_.size)
        } yield assertTrue(
          ptAfter > ptBefore + 1,
          ctAfter > ctBefore + 1,
          cAfter > cBefore + 1
        )
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[SerializationService]
          result <- svc.exportConfiguration(99999).exit
        } yield assertTrue(result.isFailure)
      },
      test("import fails for invalid json") {
        for {
          svc    <- ZIO.service[SerializationService]
          result <- svc.importConfiguration(Json.Str("invalid")).exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("multi-processor configuration roundtrip")(
      test("roundtrip preserves multiple processors with distinct controllers") {
        for {
          svc  <- ZIO.service[SerializationService]
          pt1  <- createPeripheryType("SensorA")
          pt2  <- createPeripheryType("SensorB")
          ct1  <- createControllerType(Map("p1".toPeripheryName -> pt1.id))
          ct2  <- createControllerType(Map("p2".toPeripheryName -> pt2.id))
          c1   <- createController(ct1.id)
          c2   <- createController(ct2.id)
          cfg  <- createConfiguration(
                    NonEmptySet.of(
                      FlowConfiguration.Processor(
                        unit = "UnitA",
                        parameters = Json.Obj(),
                        inbound = Chunk(Address(c1.id, "p1", "ch1", "inputA")),
                        outbound = Chunk.empty,
                        graphId = "gA"
                      ),
                      FlowConfiguration.Processor(
                        unit = "UnitB",
                        parameters = Json.Obj(),
                        inbound = Chunk(Address(c2.id, "p2", "ch1", "inputB")),
                        outbound = Chunk(Address(c1.id, "p1", "ch1", "outputB")),
                        graphId = "gB"
                      )
                    )
                  )
          json <- svc.exportConfiguration(cfg.id)
          imp  <- svc.importConfiguration(json)
        } yield assertTrue(
          imp.processors.length == 2,
          imp.processors.exists(_.unit == "UnitA"),
          imp.processors.exists(_.unit == "UnitB")
        )
      }
    )
  ).provideLayerShared(layers) @@ TestAspect.sequential
}
