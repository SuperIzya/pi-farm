package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.*
import org.pi.farm.generators.ModelGenerators as MG
import org.pi.farm.model.{Address, Controller, ControllerType, Direction, FlowConfiguration, PeripheryType}
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.service.SerializationService
import org.pi.farm.storage.*

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*

import scala.collection.immutable.SortedSet
import scala.language.implicitConversions

import cats.data.NonEmptySet

object SerializationServiceSpec extends PiFarmSpec {
  import SerializationService.*

  private val layers =
    PeripheryTypeRepositoryFake.empty ++
      ControllerTypeRepositoryFake.empty ++
      ControllerRepositoryFake.empty ++
      ConfigurationRepositoryFake.empty >+>
      SerializationService.live

  def gen[A, R, C](gen: Gen[Any, A], store: A => RIO[R, C], size: Int = 1): ZIO[R, Nothing, List[C]] =
    Gen.listOfN(size)(gen).sample.mapZIO(_.foreach(ZIO.foreach(_)(store)))

  def spec = suite("SerializationService")(
    suite("exportPeripheryType / importPeripheryType")(
      test("roundtrip preserves periphery type data") {
        check(MG.peripheryTypeNewGen) { original =>
          for {
            svc      <- ZIO.service[SerializationService]
            fake     <- ZIO.service[PeripheryTypeRepositoryFake]
            created  <- fake.create(original)
            exported  = svc.exportPeripheryType(created.id).compress
            _        <- fake.reset
            _        <- svc.importData(exported)
            imported <- fake.list()
          } yield assertTrue(
            imported.head.name == original.name,
            imported.head.description == original.description,
            imported.head.image == original.image,
            imported.head.connections == original.connections
          )
        }
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[SerializationService]
          result <- svc.exportPeripheryType(99999).runDrain.exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("exportControllerType / importControllerType")(
      test("roundtrip preserves controller type data") {
        check(MG.controllerTypeNewGen) { original =>
          for {
            pt       <- gen(MG.peripheryTypeNewGen, PeripheryTypeRepositoryFake.create, size = 4)
            
            svc      <- ZIO.service[SerializationService]
            fake     <- ZIO.service[ControllerTypeRepositoryFake]
            created  <- fake.create(original)
            exported  = svc.exportControllerType(created.id).compress
            _        <- fake.reset
            _        <- svc.importData(exported)
            imported <- fake.list()
          } yield assertTrue(
            imported.head.name == original.name,
            imported.head.description == original.description,
            imported.head.code == original.code,
            imported.head.schema == original.schema,
            imported.head.peripheries == original.peripheries
          )
        }
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
