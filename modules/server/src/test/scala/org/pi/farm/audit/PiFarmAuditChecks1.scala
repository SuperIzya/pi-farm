package org.pi.farm.audit

import org.pi.farm.fake.*
import org.pi.farm.model.{ControllerType, FlowConfiguration}
import org.pi.farm.model.FlowConfiguration.given
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.service.{ControllerTypeService, FileService, StorageService}
import org.pi.farm.storage.*

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*

import java.nio.file.{Files, Path}
import scala.collection.immutable.SortedSet
import scala.language.implicitConversions

import cats.data.NonEmptySet

object PiFarmAuditChecks1 extends DbSpec {
  def spec = suite("Audit regression checks — 2026-10-20")(
    test("same unit with different graph IDs is preserved during decoding") {
      val p       = FlowConfiguration.Processor("unit", Json.Obj(), Chunk.empty, Chunk.empty, "first")
      val second  = p.copy(graphId = "second")
      val decoded = Chunk(p, second)
        .toJson
        .fromJson[NonEmptySet[FlowConfiguration.Processor]]
      assertTrue(decoded.toOption.exists(_.size == 2), SortedSet(p, second).size == 2)
    },
    test("failed controller type update leaves the original data unchanged") {
      for {
        repo      <- ZIO.service[ControllerTypeRepository]
        periphery <- ZIO.serviceWithZIO[PeripheryTypeRepository](
                       _.create(peripheryType(1.toPeripheryTypeId))
                     )
        original  <- repo.create(
                       ControllerType.New(
                         "original",
                         "",
                         None,
                         Map("sensor".toPeripheryName -> periphery.id),
                         None
                       )
                     )
        result    <- repo
                       .update(
                         original.copy(
                           name = "changed",
                           peripheries = Map("sensor".toPeripheryName -> 999999.toPeripheryTypeId)
                         )
                       )
                       .exit
        stored    <- repo.get(original.id)
      } yield assertTrue(result.isFailure, stored.contains(original))
    }.provideLayer(controllerTypeRepositoryLayer),
    test("images use generated names and reads stay inside the static directory") {
      ZIO.scoped {
        for {
          root   <- ZIO.acquireRelease(
                      ZIO.attempt(Files.createTempDirectory(Path.of("/tmp"), "pi-farm-path-audit-"))
                    )(root =>
                      ZIO.attempt {
                        val paths = Files.walk(root)
                        try
                          paths
                            .sorted(java.util.Comparator.reverseOrder[Path]())
                            .forEach(path => Files.delete(path))
                        finally paths.close()
                      }.orDie
                    )
          base    = root.resolve("static")
          _      <- ZIO.attempt(Files.writeString(root.resolve("outside.png"), "fixture"))
          result <- (for {
                      service <- ZIO.service[FileService]
                      first   <- service.saveImage(Chunk.fromArray("fixture".getBytes))
                      second  <- service.saveImage("c2Vjb25k")
                      data    <- service.getStaticResource(first).flatMap(_._2.runCollect)
                      data2   <- service.getStaticResource(second).flatMap(_._2.runCollect)
                      read    <- service
                                   .getStaticResource("../outside.png")
                                   .flatMap(_._2.runCollect)
                                   .exit
                    } yield assertTrue(
                      first != second,
                      Path.of(first).getParent == Path.of("images"),
                      Path.of(second).getParent == Path.of("images"),
                      first.endsWith(".png"),
                      second.endsWith(".png"),
                      data == Chunk.fromArray("fixture".getBytes),
                      data2 == Chunk.fromArray("second".getBytes),
                      read.isFailure
                    )).provide(
                      FileService.live,
                      ZLayer.succeed(
                        FileService.Config(
                          base.toString,
                          "code",
                          "images",
                          root.resolve("staging").toString,
                          Chunk(
                            "platformio.ini",
                            "**/*.c",
                            "**/*.cpp",
                            "**/*.h",
                            "**/*.hpp",
                            "**/*.ino"
                          ),
                          100,
                          5_242_880
                        )
                      )
                    )
        } yield result
      }
    },
    test("export of a controller type with no peripheries succeeds") {
      (for {
        repo     <- ZIO.service[ControllerTypeRepository]
        service  <- ZIO.service[StorageService]
        original <- repo.create(ControllerType.New("empty", "", None, Map.empty, None))
        entries  <-
          service
            .exportControllerType(original.id)
            .mapZIO {
              case (entry, content) =>
                content
                  .runCollect
                  .map(bytes => entry.name -> new String(bytes.toArray).fromJson[ControllerType])
            }
            .runCollect
      } yield assertTrue(
        entries == Chunk(s"controllerTypes/${original.id}.json" -> Right(original))
      )).provide(
        StorageService.live,
        ControllerTypeService.live,
        StaticServiceFake.live,
        ControllerTypeRepositoryFake.empty,
        ControllerRepositoryFake.empty,
        PeripheryTypeRepositoryFake.empty,
        ConfigurationRepositoryFake.empty,
        ConfigurationServiceFake.empty
      )
    }
  )
}
