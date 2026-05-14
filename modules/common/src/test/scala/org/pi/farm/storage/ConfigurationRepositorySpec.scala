package org.pi.farm.storage

import org.pi.farm.generators.ModelGenerators.*
import org.pi.farm.model.{*, given}

import io.scalaland.chimney.dsl.*

import zio.*
import zio.json.ast.Json
import zio.test.*

import scala.language.implicitConversions

import cats.data.NonEmptySet

object ConfigurationRepositorySpec extends DbSpec {

  def spec = suite("ConfigurationRepositorySpec")(
    suite("CRUD Operations")(
      test("create should persist a configuration and return it with generated id") {
        check(configurationNewGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
          } yield assertTrue(
            created == configuration.into[FlowConfiguration].withFieldConst(_.id, created.id).transform
          )
        }
      },
      test("get should return Some for existing configuration") {
        check(configurationNewGen) { config =>
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
        check(configurationNewGen, configurationNewGen) { (orig, upd) =>
          for {
            original     <- prepareConfiguration(orig)
            updated      <- prepareConfiguration(upd)
            repo         <- ZIO.service[ConfigurationRepository]
            created      <- repo.create(original)
            updatedConfig = updated.into[FlowConfiguration].withFieldConst(_.id, created.id).transform
            result       <- repo.update(created.id, updatedConfig)
            retrieved    <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            result.get.processors == updatedConfig.processors,
            retrieved.isDefined,
            retrieved.get.processors == updatedConfig.processors
          )
        }
      },
      test("update should return None for non-existing configuration") {
        check(configurationWithIdGen, largeIdGen) {
          case (config, id) =>
            for {
              repo   <- ZIO.service[ConfigurationRepository]
              result <- repo.update(id, config)
            } yield assertTrue(result.isEmpty)
        }
      },
      test("delete should remove existing configuration") {
        check(configurationNewGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            deleted       <- repo.delete(created.id)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            !deleted.contains(created),
            retrieved.isEmpty
          )
        }
      },
      test("delete should return false for non-existing configuration") {
        check(largeIdGen) { nonExistentId =>
          val id: ConfigurationId = nonExistentId
          for {
            repo    <- ZIO.service[ConfigurationRepository]
            deleted <- repo.delete(nonExistentId)
          } yield assertTrue(!deleted.exists(_.id == id))
        }
      },
      test("list should return all created configurations") {
        check(Gen.listOfBounded(1, 3)(configurationNewGen)) { configs =>
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
        check(configurationNewGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.processors == created.processors,
            retrieved.get.name == created.name,
            retrieved.get.description == created.description
          )
        }
      },
      test("update-get roundtrip preserves data") {
        check(configurationNewGen, configurationNewGen) { (orig, upd) =>
          for {
            original     <- prepareConfiguration(orig)
            updated      <- prepareConfiguration(upd)
            repo         <- ZIO.service[ConfigurationRepository]
            created      <- repo.create(original)
            updatedConfig = updated
                              .into[FlowConfiguration]
                              .withFieldConst(_.id, created.id)
                              .withFieldConst(
                                _.processors,
                                created.processors.zipWith(updated.processors) {
                                  case (createdP, updatedP) =>
                                    updatedP.transformInto[FlowConfiguration.Processor]
                                }
                              )
                              .transform
            _            <- repo.update(created.id, updatedConfig)
            retrieved    <- repo.get(created.id)
          } yield assertTrue(
            retrieved.isDefined,
            retrieved.get.processors == updatedConfig.processors,
            retrieved.get.name == updatedConfig.name,
            retrieved.get.description == updatedConfig.description
          )
        }
      },
      test("create multiple and list maintains consistency") {
        check(Gen.listOfBounded(1, 3)(configurationNewGen)) { configs =>
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
        check(configurationNewGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            firstDelete   <- repo.delete(created.id)
            secondDelete  <- repo.delete(created.id)
          } yield assertTrue(
            firstDelete.toSet == secondDelete.toSet
          )
        }
      }
    ),
    suite("Processor operations")(
      test("create configuration with empty inbound and outbound") {
        check(processingUnitNameGen, jsonGen) { (unit, params) =>
          for {
            repo      <- ZIO.service[ConfigurationRepository]
            processor  = FlowConfiguration.Processor(unit, params, Chunk.empty, Chunk.empty)
            config     = FlowConfiguration.New(
                           name = "",
                           description = "",
                           processors = NonEmptySet.one(processor)
                         )
            created   <- repo.create(config)
            retrieved <- repo.get(created.id)
          } yield {
            val p = created.processors.head
            assertTrue(
              p.inbound.isEmpty,
              p.outbound.isEmpty,
              retrieved.isDefined,
              retrieved.get.processors.head.inbound.isEmpty,
              retrieved.get.processors.head.outbound.isEmpty
            )
          }
        }
      },
      test("create configuration with single inbound address") {
        check(processingUnitNameGen, jsonGen, controllerNewGen, unitsGen, nameGen) {
          (unit, params, ctrl, peripheryId, name) =>
            val pid: PeripheryId = peripheryId
            for {
              controller <- prepareController(ctrl)
              repo       <- ZIO.service[ConfigurationRepository]
              inbound     = Chunk(Address(controller.id, pid, name))
              processor   = FlowConfiguration.Processor(unit, params, inbound, Chunk.empty)
              config      = FlowConfiguration.New(
                              name = "",
                              description = "",
                              processors = NonEmptySet.one(processor)
                            )
              created    <- repo.create(config)
              retrieved  <- repo.get(created.id)
            } yield {
              val p = created.processors.head
              assertTrue(
                p.inbound.size == 1,
                p.inbound.head.controllerId == controller.id,
                retrieved.isDefined,
                retrieved.get.processors.head.inbound == p.inbound
              )
            }
        }
      },
      test("create configuration with single outbound address") {
        check(processingUnitNameGen, jsonGen, controllerNewGen, unitsGen, nameGen) { (unit, params, ctrl, id, name) =>
          val peripheryId: PeripheryId = id
          for {
            controller <- prepareController(ctrl)
            repo       <- ZIO.service[ConfigurationRepository]
            outbound    = Chunk(Address(controller.id, peripheryId, name))
            processor   = FlowConfiguration.Processor(unit, params, Chunk.empty, outbound)
            config      = FlowConfiguration.New(
                            name = "",
                            description = "",
                            processors = NonEmptySet.one(processor)
                          )
            created    <- repo.create(config)
            retrieved  <- repo.get(created.id)
          } yield {
            val p = created.processors.head
            assertTrue(
              p.outbound.size == 1,
              p.outbound.head.controllerId == controller.id,
              retrieved.isDefined,
              retrieved.get.processors.head.outbound == p.outbound
            )
          }
        }
      },
      test("create configuration with multiple processors") {
        check(configurationNewGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            created.processors.length == configuration.processors.length,
            retrieved.isDefined,
            retrieved.get.processors == created.processors
          )
        }
      },
      test("update configuration processors") {
        check(configurationNewGen, configurationNewGen) { (orig, upd) =>
          for {
            original     <- prepareConfiguration(orig)
            updated      <- prepareConfiguration(upd)
            repo         <- ZIO.service[ConfigurationRepository]
            created      <- repo.create(original)
            updatedConfig = updated.into[FlowConfiguration].withFieldConst(_.id, created.id).transform
            result       <- repo.update(created.id, updatedConfig)
            retrieved    <- repo.get(created.id)
          } yield assertTrue(
            result.isDefined,
            retrieved.isDefined,
            retrieved.get.processors == updatedConfig.processors
          )
        }
      },
      test("create configurations with different processing units") {
        check(Gen.listOfBounded(2, 4)(processingUnitNameGen)) { units =>
          for {
            repo      <- ZIO.service[ConfigurationRepository]
            configs    = units
                           .distinct
                           .map { unit =>
                             val processor = FlowConfiguration.Processor(unit, Json.Obj(), Chunk.empty, Chunk.empty)
                             FlowConfiguration.New(
                               name = "",
                               description = "",
                               processors = NonEmptySet.one(processor)
                             )
                           }
            created   <- ZIO.foreach(configs)(repo.create)
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.map(_.processors.head.unit).toSet == units.distinct.toSet,
            retrieved.forall(_.isDefined)
          )
        }
      }
    ),
    suite("Edge cases and validation")(
      test("create with various JSON parameters") {
        check(Gen.listOfBounded(3, 5)(jsonGen)) { paramsData =>
          for {
            repo      <- ZIO.service[ConfigurationRepository]
            configs    = paramsData.zipWithIndex.map {
                           case (params, idx) =>
                             val processor = FlowConfiguration.Processor(s"Unit_$idx", params, Chunk.empty, Chunk.empty)
                             FlowConfiguration.New(
                               name = "",
                               description = "",
                               processors = NonEmptySet.one(processor)
                             )
                         }
            created   <- ZIO.foreach(configs)(repo.create)
            retrieved <- ZIO.foreach(created)(c => repo.get(c.id))
          } yield assertTrue(
            created.map(_.processors.head.parameters) == paramsData,
            retrieved.forall(_.isDefined),
            retrieved.map(_.get.processors.head.parameters) == paramsData
          )
        }
      },
      test("concurrent operations maintain consistency") {
        check(Gen.listOfBounded(3, 5)(configurationNewGen)) { configs =>
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
        check(Gen.listOfBounded(1, 3)(configurationNewGen)) { configs =>
          for {
            configurations <- ZIO.foreach(configs)(prepareConfiguration)
            repo           <- ZIO.service[ConfigurationRepository]
            created        <- ZIO.foreach(configurations)(repo.create)
            validIds        = created.forall(_.id > 0)
            retrieved      <- ZIO.foreach(created)(c => repo.get(c.id))
            allFound        = retrieved.forall(_.isDefined)
          } yield assertTrue(
            validIds,
            allFound,
            created.size == configurations.size
          )
        }
      },
      test("delete operations maintain list consistency") {
        check(Gen.listOfBounded(2, 6)(configurationNewGen)) { configs =>
          for {
            configurations  <- ZIO.foreach(configs)(prepareConfiguration)
            repo            <- ZIO.service[ConfigurationRepository]
            created         <- ZIO.foreach(configurations)(repo.create)
            initialList     <- repo.list()
            toDelete         = created.take(created.size / 2)
            _               <- ZIO.foreachDiscard(toDelete)(c => repo.delete(c.id))
            finalList       <- repo.list()
            remainingCreated = created.drop(created.size / 2)
            remainingFound   = remainingCreated.forall(c => finalList.exists(_.id == c.id))
          } yield assertTrue(
            finalList.size == initialList.size - toDelete.size,
            remainingFound,
            toDelete.forall(deleted => !finalList.exists(_.id == deleted.id))
          )
        }
      } @@ TestAspect.samples(10),
      test("large address sets maintain correctness") {
        check(Gen.int(5, 15), Gen.int(5, 15), nameGen) { (inboundCount, outboundCount, name) =>
          for {
            controllers      <- ZIO.foreach((1 to (inboundCount + outboundCount)).toList) { _ =>
                                  controllerNewGen.sample.map(_.value).runHead.map(_.get).flatMap(prepareController)
                                }
            repo             <- ZIO.service[ConfigurationRepository]
            inboundAddresses  = controllers
                                  .take(inboundCount)
                                  .zipWithIndex
                                  .map {
                                    case (ctrl, idx) => Address(ctrl.id, s"inbound_$idx", name)
                                  }
                                  .to(Chunk)
            outboundAddresses = controllers
                                  .drop(inboundCount)
                                  .zipWithIndex
                                  .map {
                                    case (ctrl, idx) => Address(ctrl.id, s"outbound_$idx", name)
                                  }
                                  .to(Chunk)
            processor         = FlowConfiguration.Processor("LargeTestUnit", Json.Obj(), inboundAddresses, outboundAddresses)
            config            = FlowConfiguration.New(
                                  name = "",
                                  description = "",
                                  processors = NonEmptySet.one(processor)
                                )
            created          <- repo.create(config)
            retrieved        <- repo.get(created.id)
          } yield {
            val p = created.processors.head
            assertTrue(
              p.inbound.size == inboundCount,
              p.outbound.size == outboundCount,
              retrieved.isDefined,
              retrieved.get.processors.head.inbound.size == inboundCount,
              retrieved.get.processors.head.outbound.size == outboundCount
            )
          }
        }
      } @@ TestAspect.samples(10)
    ),
    suite("Database constraints validation")(
      test("processors are properly stored") {
        check(configurationNewGen) { config =>
          for {
            configuration <- prepareConfiguration(config)
            repo          <- ZIO.service[ConfigurationRepository]
            created       <- repo.create(configuration)
            retrieved     <- repo.get(created.id)
          } yield assertTrue(
            created.processors.forall(_.unit.nonEmpty),
            retrieved.isDefined,
            retrieved.get.processors == created.processors
          )
        }
      },
      test("foreign key relationships in address mappings") {
        check(configurationNewGen.filter(c => c.processors.exists(p => p.inbound.nonEmpty || p.outbound.nonEmpty))) {
          config =>
            for {
              configuration <- prepareConfiguration(config)
              repo          <- ZIO.service[ConfigurationRepository]
              created       <- repo.create(configuration)
              retrieved     <- repo.get(created.id)
            } yield assertTrue(
              retrieved.isDefined,
              retrieved.get.processors.forall(p => p.inbound.forall(_.controllerId > 0)),
              retrieved.get.processors.forall(p => p.outbound.forall(_.controllerId > 0)),
              retrieved.get.processors == created.processors
            )
        }
      },
      test("configuration ID auto-generation") {
        check(Gen.listOfBounded(1, 3)(configurationNewGen)) { configs =>
          for {
            configurations <- ZIO.foreach(configs)(prepareConfiguration)
            repo           <- ZIO.service[ConfigurationRepository]
            created        <- ZIO.foreach(configurations)(repo.create)
            uniqueIds       = created.map(_.id).distinct
          } yield assertTrue(
            created.forall(_.id > 0),
            uniqueIds.size == created.size
          )
        }
      },
      test("cascading delete behavior") {
        check(configurationNewGen.filter(c => c.processors.exists(p => p.inbound.nonEmpty || p.outbound.nonEmpty))) {
          config =>
            for {
              configuration <- prepareConfiguration(config)
              repo          <- ZIO.service[ConfigurationRepository]
              created       <- repo.create(configuration)
              deleted       <- repo.delete(created.id)
              retrieved     <- repo.get(created.id)
            } yield assertTrue(
              !deleted.contains(created),
              retrieved.isEmpty
            )
        }
      }
    )
  ).provideLayerShared(configurationRepositoryLayer)

  def prepareConfiguration(configuration: FlowConfiguration.New): RIO[
    ConfigurationRepository & ControllerRepository & ControllerTypeRepository & PeripheryTypeRepository,
    FlowConfiguration.New
  ] =
    for {
      preparedProcessors <- ZIO.foreach(configuration.processors.toNonEmptyList.toList) { processor =>
                              for {
                                inbound  <- ZIO.foreach(processor.inbound) { addr =>
                                              for {
                                                controller <-
                                                  controllerNewGen
                                                    .sample
                                                    .map(_.value)
                                                    .runHead
                                                    .map(_.get)
                                                    .flatMap(prepareController)
                                              } yield addr.copy(controllerId = controller.id)
                                            }
                                outbound <- ZIO.foreach(processor.outbound) { addr =>
                                              for {
                                                controller <-
                                                  controllerNewGen
                                                    .sample
                                                    .map(_.value)
                                                    .runHead
                                                    .map(_.get)
                                                    .flatMap(prepareController)
                                              } yield addr.copy(controllerId = controller.id)
                                            }
                              } yield FlowConfiguration.Processor(
                                processor.unit,
                                processor.parameters,
                                inbound,
                                outbound
                              )
                            }
    } yield configuration.copy(
      processors = NonEmptySet.fromSetUnsafe(
        scala.collection.immutable.SortedSet.from(preparedProcessors)
      )
    )

  def prepareController(
    controller: Controller.New
  ): RIO[ControllerRepository & ControllerTypeRepository & PeripheryTypeRepository, Controller] =
    for {
      controllerType <- controllerTypeNewGen.sample.map(_.value).runHead.map(_.get).flatMap(prepareControllerType)
      controllerRepo <- ZIO.service[ControllerRepository]
      prepared        = controller.copy(typeId = controllerType.id)
      created        <- controllerRepo.create(prepared)
    } yield created

  def prepareControllerType(
    controllerType: ControllerType.New
  ): RIO[ControllerTypeRepository & PeripheryTypeRepository, ControllerType] =
    for {
      ptRepo         <- ZIO.service[PeripheryTypeRepository]
      newPeripheries <- ZIO.foreachPar(controllerType.peripheries) {
                          case (id, pt) => ptRepo.create(peripheryType(pt)).map(id -> _.id)
                        }
      preparedType    = controllerType.copy(peripheries = newPeripheries)
      ctRepo         <- ZIO.service[ControllerTypeRepository]
      created        <- ctRepo.create(preparedType)
    } yield created
}
