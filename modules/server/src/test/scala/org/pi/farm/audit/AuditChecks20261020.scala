package org.pi.farm.audit

import org.pi.farm.fake.*
import org.pi.farm.model.{ControllerType, FlowConfiguration}
import org.pi.farm.model.FlowConfiguration.given
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.service.{StaticService, StorageService}
import org.pi.farm.storage.*

import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*

import java.nio.file.{Files, Path}
import scala.language.implicitConversions

import cats.data.NonEmptySet

object AuditChecks20261020 extends DbSpec {
  def spec = suite("Audit regression checks — 2026-10-20")(
    test("same unit with different graph IDs is preserved during decoding") {
      val p       = FlowConfiguration.Processor("unit", Json.Obj(), Chunk.empty, Chunk.empty, "first")
      val decoded = Chunk(p, p.copy(graphId = "second"))
        .toJson
        .fromJson[NonEmptySet[FlowConfiguration.Processor]]
      assertTrue(decoded.toOption.exists(_.size == 2))
    },
    test("failed controller type update leaves the original data unchanged") {
      for {
        repo     <- ZIO.service[ControllerTypeRepository]
        original <- repo.create(ControllerType.New("original", "", None, "", Map.empty, None))
        result   <- repo
                      .update(
                        original.copy(
                          name = "changed",
                          peripheries = Map("sensor".toPeripheryName -> 999999.toPeripheryTypeId)
                        )
                      )
                      .exit
        stored   <- repo.get(original.id)
      } yield assertTrue(result.isFailure, stored.contains(original))
    }.provideLayer(controllerTypeRepositoryLayer),
    test("image paths reject writes and reads outside the static directory") {
      for {
        root   <- ZIO.attempt(Files.createTempDirectory(Path.of("/tmp"), "pi-farm-path-audit-"))
        base    = root.resolve("static")
        _      <- ZIO.attempt(Files.createDirectories(base.resolve("images")))
        _      <- ZIO.attempt(Files.writeString(root.resolve("outside.png"), "fixture"))
        result <- (for {
                    service <- ZIO.service[StaticService]
                    write   <- service
                                 .saveImage(
                                   "images/../../escaped.png",
                                   Chunk.fromArray("fixture".getBytes)
                                 )
                                 .exit
                    read    <- service
                                 .getStaticResource("../outside.png")
                                 .flatMap(_._2.runCollect)
                                 .exit
                  } yield (write, read)).provide(
                    StaticService.live,
                    ZLayer.succeed(StaticService.Config(base.toString))
                  )
        exists <- ZIO.attempt(Files.exists(root.resolve("escaped.png")))
      } yield assertTrue(result._1.isFailure, result._2.isFailure, !exists)
    },
    test("export of a controller type with no peripheries succeeds") {
      (for {
        repo     <- ZIO.service[ControllerTypeRepository]
        service  <- ZIO.service[StorageService]
        original <- repo.create(ControllerType.New("empty", "", None, "", Map.empty, None))
        result   <- service.exportControllerType(original.id).runDrain.exit
      } yield assertTrue(result.isSuccess)).provide(
        StorageService.live,
        StaticServiceFake.live,
        ControllerTypeRepositoryFake.empty,
        ControllerRepositoryFake.empty,
        PeripheryTypeRepositoryFake.empty,
        ConfigurationRepositoryFake.empty,
        ConfigurationManagerFake.empty
      )
    }
  )
}
