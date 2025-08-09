package org.pi.farm.storage

import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.{Configuration, Controller, ControllerType, Inbound, Outbound}
import zio.*
import zio.test.*

object ConfigurationRepositorySpec extends DbSpec {

  def spec = suite("ConfigurationRepositorySpec")(
    suite("CRUD Operations")(
      test("create should persist a configuration and return it with generated id") {
        check(configurationGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
          } yield assertTrue(created == configuration.copy(id = created.id))
        }
      },
      test("get should return Some for existing configuration") {
        check(configurationGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get == created
          )
        }
      },
      test("get should return None for non-existing configuration") {
        check(largeIdGen) { nonExistentId =>
          for {
            repo      <- ZIO.service[ConfigurationRepository]
            retrieved <- repo.get(nonExistentId)
          } yield assertTrue(retrieved.isEmpty)
        }
      },
      test("update should modify existing configuration") {
        check(configurationGen, configurationGen) { (orig, upd) =>
          for {
            original <- prepareConfiguration(orig)
            updated  <- prepareConfiguration(upd)
            repo     <- ZIO.service[ConfigurationRepository]
            created  <- repo.create(original)
            updatedConfig = updated.copy(id = created.id)
            result    <- repo.update(created.id, updatedConfig)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            result.get.processingUnit == updatedConfig.processingUnit,
            result.get.additional == updatedConfig.additional,
            retrieved.isDefined,
            retrieved.get.processingUnit == updatedConfig.processingUnit
          )
        }
      },
      test("update should return None for non-existing configuration") {
        check(configurationWithIdGen, largeIdGen) { case (config, id) =>
          for {
            repo   <- ZIO.service[ConfigurationRepository]
            result <- repo.update(id, config)
          } yield assertTrue(result.isEmpty)
        }
      },
      test("delete should remove existing configuration") {
        check(configurationGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            deleted       <- repo.delete(created.id)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            deleted,
            retrieved.isEmpty
          )
        }
      },
      test("delete should return false for non-existing configuration") {
        check(largeIdGen) { nonExistentId =>
          for {
            repo    <- ZIO.service[ConfigurationRepository]
            deleted <- repo.delete(nonExistentId)
          } yield assertTrue(!deleted)
        }
      },
      test("list should return all created configurations") {
        check(Gen.listOfBounded(1, 3)(configurationGen)) { configs =>
          for {
            configurations <- ZIO.foreachPar(configs)(prepareConfiguration)
            repo           <- ZIO.service[ConfigurationRepository]
            initialCount   <- repo.list().map(_.size)
            _              <- ZIO.foreachDiscard(configurations)(repo.create)
            allConfigs     <- repo.list()
          } yield assertTrue(allConfigs.size == initialCount + configurations.size)
        }
      }
    ),
    suite("Property-based invariants")(
      test("create-get roundtrip preserves data") {
        check(configurationGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.processingUnit == configuration.processingUnit,
            retrieved.get.additional == configuration.additional,
            retrieved.get.inbound == configuration.inbound,
            retrieved.get.outbound == configuration.outbound
          )
        }
      },
      test("update-get roundtrip preserves data") {
        check(configurationGen, configurationGen) { (orig, upd) =>
          for {
            original <- prepareConfiguration(orig)
            updated  <- prepareConfiguration(upd)
            repo     <- ZIO.service[ConfigurationRepository]
            created  <- repo.create(original)
            updatedConfig = updated.copy(id = created.id)
            _         <- repo.update(created.id, updatedConfig)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.processingUnit == updatedConfig.processingUnit,
            retrieved.get.additional == updatedConfig.additional,
            retrieved.get.inbound == updatedConfig.inbound,
            retrieved.get.outbound == updatedConfig.outbound
          )
        }
      },
      test("create multiple and list maintains consistency") {
        check(Gen.listOfBounded(1, 3)(configurationGen)) { configs =>
          for {
            configurations   <- ZIO.foreachPar(configs)(prepareConfiguration)
            repo             <- ZIO.service[ConfigurationRepository]
            created          <- ZIO.foreach(configurations)(repo.create)
            allConfigs       <- repo.list()
            retrievedCreated <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.forall(c => allConfigs.exists(ac => ac.id == c.id)),
            retrievedCreated.forall(_.isDefined),
            retrievedCreated.map(_.get).toSet == created.toSet
          )
        }
      },
      test("delete is idempotent") {
        check(configurationGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            firstDelete   <- repo.delete(created.id)
            secondDelete  <- repo.delete(created.id)
          } yield assertTrue(
            firstDelete,
            !secondDelete
          )
        }
      }
    ),
    suite("Controller relationship operations")(
      test("create configuration with empty inbound and outbound sets") {
        check(processingUnitGen, Gen.option(jsonGen)) { (processingUnit, additional) =>
          for {
            repo <- ZIO.service[ConfigurationRepository]
            config = Configuration(0, Set.empty, Set.empty, processingUnit, additional)
            created   <- repo.create(config)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            created.inbound.isEmpty,
            created.outbound.isEmpty,
            retrieved.isDefined,
            retrieved.get.inbound.isEmpty,
            retrieved.get.outbound.isEmpty
          )
        }
      },
      test("create configuration with single inbound controller") {
        check(processingUnitGen, Gen.option(jsonGen), controllerGen, nameGen) {
          (processingUnit, additional, ctrl, peripheryId) =>
            for {
              controller <- prepareController(ctrl)
              repo       <- ZIO.service[ConfigurationRepository]
              inbound = Set(Inbound(controller.id, peripheryId))
              config = Configuration(0, inbound, Set.empty, processingUnit, additional)
              created   <- repo.create(config)
              retrieved <- repo.get(created.id)
            } yield assertTrue(
              created.inbound.size == 1,
              created.inbound.head.controllerId == controller.id,
              retrieved.isDefined,
              retrieved.get.inbound == created.inbound
            )
        }
      },
      test("create configuration with single outbound controller") {
        check(processingUnitGen, Gen.option(jsonGen), controllerGen, nameGen) {
          (processingUnit, additional, ctrl, peripheryId) =>
            for {
              controller <- prepareController(ctrl)
              repo       <- ZIO.service[ConfigurationRepository]
              outbound = Set(Outbound(controller.id, peripheryId))
              config = Configuration(0, Set.empty, outbound, processingUnit, additional)
              created   <- repo.create(config)
              retrieved <- repo.get(created.id)
            } yield assertTrue(
              created.outbound.size == 1,
              created.outbound.head.controllerId == controller.id,
              retrieved.isDefined,
              retrieved.get.outbound == created.outbound
            )
        }
      },
      test("create configuration with multiple controllers") {
        check(configurationGen.filter(c => c.inbound.nonEmpty && c.outbound.nonEmpty)) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            created.inbound.size == configuration.inbound.size,
            created.outbound.size == configuration.outbound.size,
            retrieved.isDefined,
            retrieved.get.inbound == created.inbound,
            retrieved.get.outbound == created.outbound
          )
        }
      },
      test("update configuration controllers") {
        check(configurationGen, configurationGen) { (orig, upd) =>
          for {
            original <- prepareConfiguration(orig)
            updated  <- prepareConfiguration(upd)
            repo     <- ZIO.service[ConfigurationRepository]
            created  <- repo.create(original)
            updatedConfig = updated.copy(id = created.id)
            result    <- repo.update(created.id, updatedConfig)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            retrieved.isDefined,
            retrieved.get.inbound == updatedConfig.inbound,
            retrieved.get.outbound == updatedConfig.outbound
          )
        }
      }
    ),
    suite("Processing unit operations")(
      test("create configurations with different processing units") {
        check(Gen.listOfBounded(2, 4)(processingUnitGen)) { processingUnits =>
          for {
            repo <- ZIO.service[ConfigurationRepository]
            configs = processingUnits.distinct.map(pu =>
              Configuration(0, Set.empty, Set.empty, pu, None)
            )
            created   <- ZIO.foreach(configs)(repo.create)
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.map(_.processingUnit).toSet == processingUnits.distinct.toSet,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get.processingUnit).toSet == processingUnits.distinct.toSet
          )
        }
      },
      test("update configuration processing unit") {
        check(configurationGen, processingUnitGen) { (config, newProcessingUnit) =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            updatedConfig = created.copy(processingUnit = newProcessingUnit)
            result        <- repo.update(created.id, updatedConfig)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            result.get.processingUnit == newProcessingUnit,
            retrieved.isDefined,
            retrieved.get.processingUnit == newProcessingUnit
          )
        }
      },
      test("filter configurations by processing unit pattern") {
        check(Gen.fromIterable(List("PingPong", "Discovery")), Gen.listOfBounded(1, 2)(processingUnitGen)) {
          (targetUnit, otherUnits) =>
            for {
              repo <- ZIO.service[ConfigurationRepository]
              targetConfigs = List.fill(2)(Configuration(0, Set.empty, Set.empty, targetUnit, None))
              otherConfigs = otherUnits.map(unit => Configuration(0, Set.empty, Set.empty, unit, None))
              allConfigs = targetConfigs ++ otherConfigs
              _           <- ZIO.foreachDiscard(allConfigs)(repo.create)
              allRetrieved <- repo.list()
              targetCount = allRetrieved.count(_.processingUnit == targetUnit)
            } yield assertTrue(targetCount >= 2) // At least our 2 target configurations
        }
      }
    ),
    suite("Edge cases and validation")(
      test("create with various JSON additional data") {
        check(Gen.listOfBounded(3, 5)(Gen.option(jsonGen))) { additionalData =>
          for {
            repo <- ZIO.service[ConfigurationRepository]
            configs = additionalData.zipWithIndex.map {
              case (additional, idx) =>
                Configuration(0, Set.empty, Set.empty, s"Unit_$idx", additional)
            }
            created   <- ZIO.foreach(configs)(repo.create)
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.map(_.additional) == additionalData,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get.additional) == additionalData
          )
        }
      },
      test("concurrent operations maintain consistency") {
        check(Gen.listOfBounded(3, 5)(configurationGen)) { configs =>
          for {
            configurations <- ZIO.foreachPar(configs)(prepareConfiguration)
            repo           <- ZIO.service[ConfigurationRepository]
            created        <- ZIO.foreachPar(configurations)(repo.create)
            retrieved      <- ZIO.foreachPar(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.size == configurations.size,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get).toSet == created.toSet
          )
        }
      },
      test("bulk operations maintain referential integrity") {
        check(Gen.listOfBounded(1, 3)(configurationGen)) { configs =>
          for {
            configurations <- ZIO.foreach(configs)(prepareConfiguration)
            repo           <- ZIO.service[ConfigurationRepository]
            created        <- ZIO.foreach(configurations)(repo.create)
            // Verify all created configurations have valid IDs
            validIds = created.forall(_.id > 0)
            // Verify all can be retrieved
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
            allFound = retrieved.forall(_.isDefined)
          } yield assertTrue(
            validIds,
            allFound,
            created.size == configurations.size
          )
        }
      },
      test("delete operations maintain list consistency") {
        check(Gen.listOfBounded(2, 6)(configurationGen)) { configs =>
          for {
            configurations <- ZIO.foreach(configs)(prepareConfiguration)
            repo           <- ZIO.service[ConfigurationRepository]
            created        <- ZIO.foreach(configurations)(repo.create)
            initialList    <- repo.list()
            // Delete half of the configurations
            toDelete = created.take(created.size / 2)
            _        <- ZIO.foreachDiscard(toDelete)(c => repo.delete(c.id))
            finalList <- repo.list()
            remainingCreated = created.drop(created.size / 2)
            remainingFound = remainingCreated.forall(c => finalList.exists(_.id == c.id))
          } yield assertTrue(
            finalList.size == initialList.size - toDelete.size,
            remainingFound,
            toDelete.forall(deleted => !finalList.exists(_.id == deleted.id))
          )
        }
      }@@ TestAspect.samples(25),
      test("large controller sets maintain performance") {
        check(Gen.int(5, 15), Gen.int(5, 15)) { (inboundCount, outboundCount) =>
          for {
            controllers <- ZIO.foreach((1 to (inboundCount + outboundCount)).toList) { _ =>
              controllerGen.sample.map(_.value).runHead.map(_.get).flatMap(prepareController)
            }
            repo <- ZIO.service[ConfigurationRepository]
            inboundControllers = controllers.take(inboundCount).zipWithIndex.map {
              case (ctrl, idx) => Inbound(ctrl.id, s"inbound_$idx")
            }.toSet
            outboundControllers = controllers.drop(inboundCount).zipWithIndex.map {
              case (ctrl, idx) => Outbound(ctrl.id, s"outbound_$idx")
            }.toSet
            config = Configuration(0, inboundControllers, outboundControllers, "LargeTestUnit", None)
            created   <- repo.create(config)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            created.inbound.size == inboundCount,
            created.outbound.size == outboundCount,
            retrieved.isDefined,
            retrieved.get.inbound.size == inboundCount,
            retrieved.get.outbound.size == outboundCount
          )
        }
      } @@ TestAspect.samples(10)
    ),
    suite("Database constraints validation")(
      test("processing unit is properly stored") {
        check(configurationGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            created.processingUnit.nonEmpty,
            retrieved.isDefined,
            retrieved.get.processingUnit == created.processingUnit
          )
        }
      },
      test("foreign key relationships in controller mappings") {
        check(configurationGen.filter(c => c.inbound.nonEmpty || c.outbound.nonEmpty)) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.inbound.forall(_.controllerId > 0), // Valid foreign keys
            retrieved.get.outbound.forall(_.controllerId > 0), // Valid foreign keys
            retrieved.get.inbound == created.inbound,
            retrieved.get.outbound == created.outbound
          )
        }
      },
      test("configuration ID auto-generation") {
        check(Gen.listOfBounded(1, 3)(configurationGen)) { configs =>
          for {
            configurations <- ZIO.foreach(configs)(prepareConfiguration)
            repo           <- ZIO.service[ConfigurationRepository]
            created        <- ZIO.foreach(configurations)(repo.create)
            uniqueIds = created.map(_.id).distinct
          } yield assertTrue(
            created.forall(_.id > 0),
            uniqueIds.size == created.size, // All IDs are unique
            created.zip(configurations).forall { case (created, original) =>
              created.processingUnit == original.processingUnit
            }
          )
        }
      },
      test("cascading delete behavior") {
        check(configurationGen.filter(c => c.inbound.nonEmpty || c.outbound.nonEmpty)) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            // Delete the configuration
            deleted   <- repo.delete(created.id)
            retrieved <- repo.get(created.id)
          } yield assertTrue(
            deleted,
            retrieved.isEmpty
            // Note: Controller relationships should also be cleaned up via CASCADE
          )
        }
      },
      test("JSON additional data handling") {
        check(configurationGen.filter(_.additional.isDefined)) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            created.additional == configuration.additional,
            retrieved.isDefined,
            retrieved.get.additional == created.additional
          )
        }
      }
    )
  ).provideLayerShared(configurationRepositoryLayer)

  def prepareConfiguration(configuration: Configuration): RIO[ConfigurationRepository & ControllerRepository & ControllerTypeRepository & PeripheryTypeRepository, Configuration] =
    for {
      // Prepare inbound controllers
      inboundControllers <- ZIO.foreach(configuration.inbound) { inbound =>
        for {
          controller <- controllerGen.sample.map(_.value).runHead.map(_.get).flatMap(prepareController)
        } yield inbound.copy(controllerId = controller.id)
      }
      // Prepare outbound controllers
      outboundControllers <- ZIO.foreach(configuration.outbound) { outbound =>
        for {
          controller <- controllerGen.sample.map(_.value).runHead.map(_.get).flatMap(prepareController)
        } yield outbound.copy(controllerId = controller.id)
      }
    } yield configuration.copy(
      inbound = inboundControllers,
      outbound = outboundControllers
    )

  def prepareController(controller: Controller): RIO[ControllerRepository & ControllerTypeRepository & PeripheryTypeRepository, Controller] =
    for {
      // Create a controller type first
      controllerType <- controllerTypeGen.sample.map(_.value).runHead.map(_.get).flatMap(prepareControllerType)
      controllerRepo <- ZIO.service[ControllerRepository]
      prepared = controller.copy(typeId = controllerType.id)
      created <- controllerRepo.create(prepared)
    } yield created

  def prepareControllerType(controllerType: ControllerType): RIO[ControllerTypeRepository & PeripheryTypeRepository, ControllerType] =
    for {
      ptRepo         <- ZIO.service[PeripheryTypeRepository]
      newPeripheries <- ZIO.foreachPar(controllerType.periphery) {
        case (id, pt) => ptRepo.create(pt).map(id -> _)
      }
      preparedType = controllerType.copy(periphery = newPeripheries)
      ctRepo  <- ZIO.service[ControllerTypeRepository]
      created <- ctRepo.create(preparedType)
    } yield created
}
