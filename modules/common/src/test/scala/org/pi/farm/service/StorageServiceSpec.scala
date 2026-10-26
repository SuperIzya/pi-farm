package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.{
  ConfigurationRepositoryFake,
  ConfigurationServiceFake,
  ControllerRepositoryFake,
  ControllerTypeRepositoryFake,
  PeripheryTypeRepositoryFake,
  StaticServiceFake
}
import org.pi.farm.generators.ModelGenerators.{nameStrGen, peripheryNameGen}
import org.pi.farm.generators.ModelGenerators as MG
import org.pi.farm.model.{
  Address,
  Controller,
  ControllerType,
  Direction,
  FlowConfiguration,
  PeripheryType
}
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.storage.*

import io.scalaland.chimney.dsl.*

import zio.*
import zio.compress.ArchiveEntry
import zio.internal.stacktracer.SourceLocation
import zio.json.*
import zio.json.ast.Json
import zio.stream.ZStream
import zio.test.*
import zio.test.Assertion.equalTo

import java.nio.file.{Files, Path}
import java.util.Base64
import scala.collection.immutable.SortedSet
import scala.language.implicitConversions

import cats.data.NonEmptySet

object StorageServiceSpec extends PiFarmSpec {
  override def aspects =
    Chunk(
      TestAspect.shrinks(1),
      TestAspect.forked,
      TestAspect.samples(20),
      TestAspect.sequential,
      TestAspect.timeout(1.minute),
      TestAspect.timed
    )

  import org.pi.farm.utils.Archive.*

  import StorageService.*

  private def clearPeripheryTypes: ZIO[PeripheryTypeRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[PeripheryTypeRepository](_.list()).flatMap { entities =>
      ZIO.foreachDiscard(entities)(entity =>
        ZIO.serviceWithZIO[PeripheryTypeRepository](_.delete(entity.id))
      )
    }

  private def clearControllerTypes: ZIO[ControllerTypeRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[ControllerTypeRepository](_.list()).flatMap { entities =>
      ZIO.foreachDiscard(entities)(entity =>
        ZIO.serviceWithZIO[ControllerTypeRepository](_.delete(entity.id))
      )
    }

  private def clearControllers: ZIO[ControllerRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[ControllerRepository](_.list()).flatMap { entities =>
      ZIO.foreachDiscard(entities)(entity =>
        ZIO.serviceWithZIO[ControllerRepository](_.delete(entity.id))
      )
    }

  private def clearConfigurations: ZIO[ConfigurationRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[ConfigurationRepository](_.list()).flatMap { entities =>
      ZIO.foreachDiscard(entities)(entity =>
        ZIO.serviceWithZIO[ConfigurationRepository](_.delete(entity.id))
      )
    }

  private def createPeripheryType(entity: PeripheryType) =
    ZIO.serviceWithZIO[StorageService](
      _.savePeripheryType(entity.transformInto[PeripheryType.New])
    )

  private def createControllerType(
    data: (ControllerType, Set[PeripheryType])
  ) = {
    val (entity, peripheries) = data
    for {
      idsMap  <- ZIO
                   .foreach(peripheries) { p =>
                     createPeripheryType(p).map(n => p.id -> n.id)
                   }
                   .map(_.toMap)
      created <- ZIO.serviceWithZIO[ControllerTypeRepository](
                   _.create(
                     entity
                       .into[ControllerType.New]
                       .withFieldComputed(_.peripheries, _.peripheries.view.mapValues(idsMap).toMap)
                       .transform
                   )
                 )
      _       <- ZIO.serviceWithZIO[StaticServiceFake](
                   _.addResource(
                     Path.of("code", s"${created.id}.tar.gz"),
                     Chunk.fromArray("test firmware".getBytes)
                   )
                 )
    } yield (created, idsMap)
  }

  private def createController(
    data: (Controller, ControllerType, Set[PeripheryType])
  ) = {
    val (entity, ctrlType, peripheries) = data
    for {
      (controllerType, _) <- createControllerType((ctrlType, peripheries))
      created             <- ZIO.serviceWithZIO[ControllerRepository](
                               _.create(
                                 entity
                                   .into[Controller.New]
                                   .withFieldConst(_.typeId, controllerType.id)
                                   .transform
                               )
                             )
    } yield created
  }

  private def createConfiguration(
    data: Chunk[(Controller, ControllerType, Set[PeripheryType])],
    processors: NonEmptySet[FlowConfiguration.Processor]
  ) =
    for {
      ctlsIds <- ZIO
                   .foreach(data) {
                     case (ctrl, ct, peripheries) =>
                       createController((ctrl, ct, peripheries)).map(n => ctrl.id -> n.id)
                   }
                   .map(_.toMap)

      updated = processors
                  .map(p =>
                    p.copy(
                      inbound =
                        p.inbound.map { a => a.copy(controllerId = ctlsIds(a.controllerId)) },
                      outbound =
                        p.outbound.map { a => a.copy(controllerId = ctlsIds(a.controllerId)) }
                    )
                  )

      create <- ZIO.serviceWithZIO[ConfigurationRepository](
                  _.create(
                    FlowConfiguration.New(
                      name = "TestConfiguration",
                      description = "Test configuration",
                      previewSvg = None,
                      graphData = Json.Obj(),
                      processors = updated
                    )
                  )
                )
    } yield create

  private def genPeripheries: Gen[Any, List[PeripheryType]] =
    Gen.listOfN(4)(MG.peripheryTypeGen)

  private def genPeripheryType(name: String): Gen[Any, PeripheryType] =
    MG.peripheryTypeGen.map(_.copy(name = name))

  private def genControllerType(
    genPt: Gen[Any, PeripheryType] = nameStrGen.flatMap(genPeripheryType),
    genPeripheries: Gen[Any, PeripheryType]*
  ): Gen[Any, (ControllerType, Set[PeripheryType])] =
    for {
      peripheries <- genPeripheries.foldLeft(genPt.map(Set(_)))(_.zipWith(_)((a, b) => a + b))
      ct          <-
        MG.controllerTypeGen
          .map(
            _.copy(peripheries =
              peripheries.zipWithIndex.map { case (p, i) => s"p$i".toPeripheryName -> p.id }.toMap
            )
          )
    } yield (ct, peripheries)

  private def genController(
    genPt: Gen[Any, PeripheryType] = nameStrGen.flatMap(genPeripheryType),
    genPeripheries: Gen[Any, PeripheryType]*
  ): Gen[
    Any,
    (Controller, ControllerType, Set[PeripheryType])
  ] =
    for {
      (ct, peripheries) <- genControllerType(genPt, genPeripheries*)
      created           <- MG.controllerGen
                             .map(_.copy(typeId = ct.id))
    } yield (created, ct, peripheries)

// -- Builds a full object graph: periphery -> controller type -> controller -> configuration --
  private def buildFullGraph(data: (Controller, ControllerType, Set[PeripheryType])) =
    createConfiguration(
      Chunk(data),
      NonEmptySet.one(
        FlowConfiguration.Processor(
          unit = "TestUnit",
          parameters = Json.Obj("key" -> Json.Str("value")),
          inbound = Chunk(Address(data._1.id, "p0", "ch1", "input1")),
          outbound = Chunk.empty,
          graphId = "graph1"
        )
      )
    )

  def exportData(
    f: StorageService => EntryStream[Some]
  ): ZIO[StorageService, Throwable, ContentStream] =
    for {
      svc      <- ZIO.service[StorageService]
      exported <- f(svc).compress.runCollect
    } yield ZStream.fromChunk(exported)

  def compareImages(original: PeripheryType, imported: PeripheryType)(using Trace, SourceLocation) =
    for {
      importedImage      <- ZIO
                              .serviceWithZIO[FileService](_.getStaticResource(imported.image))
                              .flatMap(_._2.runCollect)
      importedImageBase64 = Base64.getEncoder.encodeToString(importedImage.toArray)
      originalImage      <- ZIO
                              .serviceWithZIO[FileService](_.getStaticResource(original.image))
                              .flatMap(_._2.runCollect)
      originalImageBase64 = Base64.getEncoder.encodeToString(originalImage.toArray)
    } yield assert(importedImageBase64)(equalTo(originalImageBase64))

  private def archive(name: String, size: Long, content: ContentStream): ContentStream = {
    val entries: EntryStream[Some] = ZStream.succeed(ArchiveEntry(name, Some(size)) -> content)
    entries.compress
  }

  private def imageArchive: ContentStream =
    archive("images/test.png", 3L, ZStream.fromIterable(Array[Byte](1, 2, 3)))

  def spec = suite("StorageService")(
    suite("temporary import files")(
      test("stores upload and extracted contents in /tmp and removes them after use") {
        val size    = 8L * 1024 * 1024
        val content = ZStream.repeatZIOChunk(ZIO.succeed(Chunk.fill[Byte](8192)(0))).take(size)
        for {
          extracted <- ZIO.scoped {
                         for {
                           entries <- archive("images/large.png", size, content).decompress
                           path     = entries.head._2
                           root     = path.getParent.getParent.getParent
                           stored  <- ZIO.attemptBlocking {
                                        root.getParent == Path.of("/tmp") &&
                                        Files.size(root.resolve("upload.tar.gz")) > 0 &&
                                        Files.size(path) == size
                                      }
                         } yield (root, stored)
                       }
          removed   <- ZIO.attemptBlocking(!Files.exists(extracted._1))
        } yield assertTrue(extracted._2, removed)
      },
      test("removes temporary files when importing fails") {
        for {
          root   <- Ref.make(Option.empty[Path])
          result <- ZIO.scoped {
                      for {
                        entries <- imageArchive.decompress
                        _       <- root.set(Some(entries.head._2.getParent.getParent.getParent))
                        _       <- ZIO.fail(new Exception("Import failed"))
                      } yield ()
                    }.exit
          path   <- root.get.someOrFail(new Exception("Archive was not extracted"))
          exists <- ZIO.attemptBlocking(Files.exists(path))
        } yield assertTrue(result.isFailure, !exists)
      },
      test("removes temporary files when importing is interrupted") {
        for {
          extracted <- Promise.make[Nothing, Path]
          fiber     <- ZIO.scoped {
                         for {
                           entries <- imageArchive.decompress
                           _       <- extracted.succeed(entries.head._2.getParent.getParent.getParent)
                           _       <- ZIO.never
                         } yield ()
                       }.fork
          root      <- extracted.await
          _         <- fiber.interrupt
          exists    <- ZIO.attemptBlocking(Files.exists(root))
        } yield assertTrue(!exists)
      },
      test("rejects archive paths outside the extraction directory") {
        ZIO
          .foreach(List("../escape.png", "images/../../escape.png")) { name =>
            ZIO.scoped(archive(name, 0L, ZStream.empty).decompress).exit.map { result =>
              assertTrue(
                result
                  .causeOption
                  .flatMap(_.failureOption)
                  .exists(
                    _.getMessage.contains("Archive entry outside import directory")
                  )
              )
            }
          }
          .map(_.reduce(_ && _))
      }
    ),
    suite("exportPeripheryType / importPeripheryType")(
      test("roundtrip preserves periphery type data") {
        check(MG.peripheryTypeGen) { original =>
          for {
            svc      <- ZIO.service[StorageService]
            fake     <- ZIO.service[PeripheryTypeRepository]
            created  <- createPeripheryType(original)
            exported <- exportData(_.exportPeripheryType(created.id))
            _        <- clearPeripheryTypes
            _        <- svc.importData(exported)
            imported <- fake.list()
            images   <- compareImages(created, imported.head)
          } yield assertTrue(
            imported.size == 1,
            imported.head.name == original.name,
            imported.head.description == original.description,
            imported.head.connections == original.connections
          ) && images
        }
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[StorageService]
          result <- svc.exportPeripheryType(99999).runDrain.exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("exportControllerType / importControllerType")(
      test("roundtrip preserves controller type data") {
        check(genControllerType()) { original =>
          for {
            svc    <- ZIO.service[StorageService]
            fake   <- ZIO.service[ControllerTypeRepository]
            ptRepo <- ZIO.service[PeripheryTypeRepository]

            (created, ptMap) <- createControllerType(original)
            expectedPt       <- ZIO
                                  .foreach(ptMap.values)(ptRepo.get)
                                  .map(Chunk.from(_).flatten)
            exported         <- exportData(_.exportControllerType(created.id))
            // Keep original images for the byte comparison after import.
            _                <- clearControllerTypes
            _                <- clearPeripheryTypes
            _                <- svc.importData(exported)
            imported         <- fake.list()
            importedPt       <- ZIO
                                  .foreach(imported.head.peripheries.values)(ptRepo.get)
                                  .map(Chunk.from(_).flatten)

            images <- ZIO.foreach(expectedPt.zip(importedPt))(compareImages)
          } yield assertTrue(
            imported.size == 1,
            imported.head.name == created.name,
            imported.head.description == created.description,
            imported.head.schema == created.schema,
            imported.head.peripheries.keySet == created.peripheries.keySet,
            importedPt.map(_.into[PeripheryType.New].withFieldConst(_.image, "").transform) ==
              expectedPt.map(_.into[PeripheryType.New].withFieldConst(_.image, "").transform)
          ) && images.reduce(_ && _)
        }
      },
      test("roundtrip preserves controller type firmware when present") {
        check(genControllerType()) { original =>
          for {
            svc              <- ZIO.service[StorageService]
            repo             <- ZIO.service[ControllerTypeRepository]
            static           <- ZIO.service[StaticServiceFake]
            (created, _)     <- createControllerType(original)
            firmware          = Chunk.fromArray("firmware archive".getBytes)
            _                <- static.addResource(Path.of("code", s"${created.id}.tar.gz"), firmware)
            exported         <- exportData(_.exportControllerType(created.id))
            _                <- clearControllerTypes
            _                <- svc.importData(exported)
            imported         <- repo.list()
            importedFirmware <-
              ZIO
                .serviceWithZIO[FileService](
                  _.getStaticResource(Path.of("code", s"${imported.head.id}.tar.gz"))
                )
                .flatMap(_._2.runCollect)
          } yield assertTrue(imported.size == 1, importedFirmware == firmware)
        }
      },
      test("roundtrip preserves multiple peripheries for controller type") {
        check(
          genControllerType(
            genPeripheryType("SensorA"),
            genPeripheryType("SensorB")
          )
        ) { original =>
          for {
            svc           <- ZIO.service[StorageService]
            fake          <- ZIO.service[ControllerTypeRepository]
            (expected, _) <- createControllerType(original)
            exported      <- exportData(_.exportControllerType(expected.id))
            _             <- clearControllerTypes
            imported      <- svc.importData(exported)
            imported      <- fake.list()
          } yield assertTrue(
            imported.size == 1,
            imported.head.peripheries.size == 2,
            imported.head.peripheries.keySet == expected.peripheries.keySet
          )
        }
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[StorageService]
          result <- svc.exportControllerType(99999).runDrain.exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("exportController / importController")(
      test("roundtrip preserves controller data") {
        check(genController()) { original =>
          for {
            svc      <- ZIO.service[StorageService]
            expected <- createController(original)
            exported <- exportData(_.exportController(expected.id))
            fake     <- ZIO.service[ControllerRepository]
            _        <- clearControllers
            _        <- svc.importData(exported)
            imported <- fake.list()
          } yield assertTrue(
            imported.head.name == expected.name,
            imported.head.description == expected.description,
            imported.head.id != expected.id
          )
        }
      },
      test("imported controller gets new controller type and periphery type ids") {
        check(genController()) { original =>
          for {
            svc          <- ZIO.service[StorageService]
            ptRepo       <- ZIO.service[PeripheryTypeRepository]
            ctRepo       <- ZIO.service[ControllerTypeRepository]
            expected     <- createController(original)
            expectedType <- ctRepo.get(expected.typeId)
            exported     <- exportData(_.exportController(expected.id))
            fake         <- ZIO.service[ControllerRepository]
            _            <- clearControllers
            _            <- svc.importData(exported)
            imported     <- fake.list()
            // the imported controller should reference a new controller type
            newCt        <- ctRepo.get(imported.head.typeId)
          } yield assertTrue(
            imported.size == 1,
            imported.head.name == expected.name,
            imported.head.description == expected.description,
            imported.head.id != expected.id,
            imported.head.typeId != expected.typeId,
            newCt.isDefined,
            newCt.get.name == expectedType.get.name
          )
        }
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[StorageService]
          result <- svc.exportController(99999).runDrain.exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("exportConfiguration / importConfiguration")(
      test("roundtrip preserves configuration data") {
        check(genController()) { controller =>
          for {
            config   <- buildFullGraph(controller)
            svc      <- ZIO.service[StorageService]
            exported <- exportData(_.exportConfiguration(config.id))
            fake     <- ZIO.service[ConfigurationRepository]
            _        <- clearConfigurations
            _        <- svc.importData(exported)
            imported <- fake.list()
          } yield assertTrue(
            imported.size == 1,
            imported.head.name == config.name,
            imported.head.description == config.description,
            imported.head.processors.length == config.processors.length,
            imported.head.processors.head.unit == config.processors.head.unit,
            imported.head.processors.head.parameters == config.processors.head.parameters,
            imported.head.id != config.id
          )
        }
      },
      test("imported configuration references new controller ids") {
        check(genController()) { controller =>
          for {
            config     <- buildFullGraph(controller)
            svc        <- ZIO.service[StorageService]
            exported   <- exportData(_.exportConfiguration(config.id))
            fake       <- ZIO.service[ConfigurationRepository]
            _          <- clearAll
            _          <- svc.importData(exported)
            imported   <- fake.list()
            originalCId = config.processors.head.inbound.head.controllerId
            importedCId = imported.head.processors.head.inbound.head.controllerId
          } yield assertTrue(importedCId != originalCId)
        }
      },
      test(
        "imported configuration creates new periphery types, controller types, and controllers"
      ) {
        check(genController()) { controller =>
          for {
            config   <- buildFullGraph(controller)
            svc      <- ZIO.service[StorageService]
            ptRepo   <- ZIO.service[PeripheryTypeRepository]
            ctRepo   <- ZIO.service[ControllerTypeRepository]
            cRepo    <- ZIO.service[ControllerRepository]
            ptBefore <- ptRepo.list().map(_.size)
            ctBefore <- ctRepo.list().map(_.size)
            cBefore  <- cRepo.list().map(_.size)
            exported <- exportData(_.exportConfiguration(config.id))

            _       <- svc.importData(exported)
            ptAfter <- ptRepo.list().map(_.size)
            ctAfter <- ctRepo.list().map(_.size)
            cAfter  <- cRepo.list().map(_.size)
          } yield assertTrue(
            ptAfter > ptBefore,
            ctAfter > ctBefore,
            cAfter > cBefore
          )
        }
      },
      test("export fails for nonexistent id") {
        for {
          svc    <- ZIO.service[StorageService]
          result <- svc.exportConfiguration(99999).runDrain.exit
        } yield assertTrue(result.isFailure)
      }
    ),
    suite("multi-processor configuration roundtrip")(
      test("roundtrip preserves multiple processors with distinct controllers") {
        check(
          genController(genPeripheryType("SensorA")),
          genController(genPeripheryType("SensorB"))
        ) {
          case (c1, c2) =>
            for {
              svc      <- ZIO.service[StorageService]
              cfg      <- createConfiguration(
                            Chunk(c1, c2),
                            NonEmptySet.of(
                              FlowConfiguration.Processor(
                                unit = "UnitA",
                                parameters = Json.Obj("key" -> Json.Str("value")),
                                inbound = Chunk(Address(c1._1.id, "p0", "ch1", "inputA")),
                                outbound = Chunk.empty,
                                graphId = "gA"
                              ),
                              FlowConfiguration.Processor(
                                unit = "UnitB",
                                parameters = Json.Obj("key" -> Json.Str("value")),
                                inbound = Chunk(Address(c2._1.id, "p0", "ch1", "inputB")),
                                outbound = Chunk(Address(c1._1.id, "p0", "ch1", "outputB")),
                                graphId = "gB"
                              )
                            )
                          )
              exported <- exportData(_.exportConfiguration(cfg.id))
              fake     <- ZIO.service[ConfigurationRepository]
              _        <- clearAll

              _   <- svc.importData(exported)
              imp <- fake.list()
            } yield assertTrue(
              imp.size == 1,
              imp.head.processors.length == 2,
              imp.head.processors.exists(_.unit == "UnitA"),
              imp.head.processors.exists(_.unit == "UnitB")
            )
        }
      }
    )
  ).provide(
    ConfigurationRepositoryFake.empty,
    ConfigurationServiceFake.empty,
    ControllerRepositoryFake.empty,
    ControllerTypeRepositoryFake.empty,
    PeripheryTypeRepositoryFake.empty,
    StaticServiceFake.live,
    ControllerTypeService.live,
    StorageService.live
  )

  private val clearAll =
    clearConfigurations *> clearControllers *> clearControllerTypes *> clearPeripheryTypes *>
      ZIO.serviceWithZIO[StaticServiceFake](_.reset)
}
