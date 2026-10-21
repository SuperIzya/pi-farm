package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.*
import org.pi.farm.model.ControllerType
import org.pi.farm.model.Types.*
import org.pi.farm.storage.ControllerTypeRepository
import org.pi.farm.ws.Command

import zio.*
import zio.compress.{ArchiveEntry, GzipDecompressor, TarUnarchiver}
import zio.json.*
import zio.stream.ZStream
import zio.test.*

import java.nio.file.{Files, Path}

object ControllerTypeManagerSpec extends PiFarmSpec {
  import StorageService.*
  private val data = ControllerType.New("board".toName, "", None, Map.empty, None)

  private case class Fixture(
    root: Path,
    storage: StaticService,
    repository: ControllerTypeRepository,
    manager: ControllerTypeManager
  ) {
    def stage(text: String = "[env]"): Task[Path] =
      storage
        .stageCode(
          ZStream.succeed(
            "platformio.ini" -> ZStream.fromIterable(text.getBytes)
          )
        )
        .map(name => root.resolve("staging").resolve(name))

    def bytes(id: ControllerTypeId): Task[Chunk[Byte]] =
      storage
        .getCodeArchive(id)
        .someOrFail(new Exception("Missing archive"))
        .flatMap(_._2.runCollect)

    def serialization: ZIO[Scope, Throwable, StorageService] =
      ZLayer
        .make[StorageService](
          ZLayer.succeed(storage),
          ZLayer.succeed(repository),
          ZLayer.succeed(manager),
          PeripheryTypeRepositoryFake.empty,
          ControllerRepositoryFake.empty,
          ConfigurationRepositoryFake.empty,
          ConfigurationManagerFake.empty,
          StorageService.live
        )
        .build
        .map(_.get[StorageService])
  }

  private val fixture: ZIO[Scope, Throwable, Fixture] =
    for {
      root <- ZIO.acquireRelease(
                ZIO.attemptBlocking(Files.createTempDirectory("pi-farm-lifecycle-"))
              )(root =>
                ZStream
                  .fromJavaStream(Files.walk(root))
                  .runCollect
                  .flatMap(paths =>
                    ZIO.foreachDiscard(paths.reverse)(p => ZIO.attemptBlocking(Files.delete(p)))
                  )
                  .orDie
              )
      env  <- ZLayer
                .make[StaticService & ControllerTypeRepository & ControllerTypeManager](
                  StaticService.live,
                  ControllerTypeRepositoryFake.empty,
                  ControllerTypeManager.live,
                  ZLayer.succeed(
                    StaticService.Config(
                      root.toString,
                      "firmware",
                      "images",
                      root.resolve("staging").toString,
                      3,
                      64
                    )
                  )
                )
                .build
    } yield Fixture(
      root,
      env.get[StaticService],
      env.get[ControllerTypeRepository],
      env.get[ControllerTypeManager]
    )

  def spec = suite("ControllerTypeManager")(
    test("creates with firmware and preserves it on metadata-only updates") {
      ZIO.scoped {
        for {
          f       <- fixture
          archive <- f.stage()
          created <- f.manager.create(data, archive)
          before  <- f.bytes(created.id)
          updated <- f.manager.update(created.copy(description = "changed"))
          after   <- f.bytes(created.id)
          stored  <- f.repository.get(created.id)
        } yield assertTrue(
          before.nonEmpty,
          after == before,
          stored == updated,
          updated.exists(_.description == "changed")
        )
      }
    },
    test("invalid and missing archives fail before creating records") {
      ZIO.scoped {
        for {
          f       <- fixture
          _       <- ZIO.attemptBlocking(Files.writeString(f.root.resolve("bad.tar.gz"), "not gzip"))
          invalid <- f.manager.create(data, f.root.resolve("bad.tar.gz")).exit
          missing <- f.manager.create(data, f.root.resolve("missing.tar.gz")).exit
          records <- f.repository.list()
        } yield assertTrue(invalid.isFailure, missing.isFailure, records.isEmpty)
      }
    },
    test("failed validation leaves metadata and existing firmware unchanged") {
      ZIO.scoped {
        for {
          f       <- fixture
          archive <- f.stage()
          created <- f.manager.create(data, archive)
          before  <- f.bytes(created.id)
          result  <-
            f.manager
              .update(created.copy(description = "bad"), Some(f.root.resolve("missing")))
              .exit
          after   <- f.bytes(created.id)
          stored  <- f.repository.get(created.id)
        } yield assertTrue(result.isFailure, before == after, stored.contains(created))
      }
    },
    test("successive replacements overwrite existing firmware without a revision") {
      ZIO.scoped {
        for {
          f            <- fixture
          archive      <- f.stage()
          created      <- f.manager.create(data, archive)
          first        <- f.stage("[env:first]")
          second       <- f.stage("[env:second]")
          firstUpdate  <- f.manager.update(created, Some(first))
          firstBytes   <- f.bytes(created.id)
          secondUpdate <- f.manager.update(created, Some(second))
          contents     <-
            f.bytes(created.id).flatMap { bytes =>
              ZStream
                .fromChunk(bytes)
                .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
                .mapZIO {
                  case (entry, content) =>
                    content.runCollect.map(bytes => entry.name -> new String(bytes.toArray))
                }
                .runCollect
            }
          secondBytes  <- f.bytes(created.id)
        } yield assertTrue(
          firstUpdate.contains(created),
          secondUpdate.contains(created),
          firstBytes != secondBytes,
          contents == Chunk("platformio.ini" -> "[env:second]")
        )
      }
    },
    test("publication failure is explicit, retains the record, and a later upload repairs it") {
      ZIO.scoped {
        for {
          f        <- fixture
          archive  <- f.stage()
          _        <- ZIO.attemptBlocking(Files.createDirectories(f.root.resolve("firmware/1001.tar.gz")))
          result   <- f.manager.create(data, archive).exit
          records  <- f.repository.list()
          _        <- ZIO.attemptBlocking(Files.delete(f.root.resolve("firmware/1001.tar.gz")))
          repaired <- f.manager.update(records.head, Some(archive))
          bytes    <- f.bytes(records.head.id)
        } yield assertTrue(result.isFailure, records.size == 1, repaired.isDefined, bytes.nonEmpty)
      }
    },
    test("deletes the record and its archive") {
      ZIO.scoped {
        for {
          f         <- fixture
          archive   <- f.stage()
          created   <- f.manager.create(data, archive)
          remaining <- f.manager.delete(created.id)
          present   <- f.repository.exists(created.id)
          code      <- f.storage.getCodeArchive(created.id)
        } yield assertTrue(remaining.isEmpty, !present, code.isEmpty)
      }
    },
    test(
      "archive deletion failures do not fail database deletion and leave leftovers for cleanup"
    ) {
      ZIO.scoped {
        for {
          f         <- fixture
          created   <- f.repository.create(data)
          _         <- ZIO.attemptBlocking {
                         val directory = f.root.resolve(s"firmware/${created.id}.tar.gz")
                         Files.createDirectories(directory)
                         Files.writeString(directory.resolve("blocker"), "")
                       }
          remaining <- f.manager.delete(created.id)
          present   <- f.repository.exists(created.id)
          leftover  <- ZIO.attemptBlocking(
                         Files.exists(f.root.resolve(s"firmware/${created.id}.tar.gz/blocker"))
                       )
        } yield assertTrue(remaining.isEmpty, !present, leftover)
      }
    },
    test("database failures leave existing firmware and metadata untouched") {
      ZIO.scoped {
        for {
          f           <- fixture
          archive     <- f.stage()
          created     <- f.manager.create(data, archive)
          before      <- f.bytes(created.id)
          replacement <- f.stage("[env:replacement]")
          failing      = new ControllerTypeRepository {
                           export f.repository.{create, exists, get, list}
                           def update(data: ControllerType): Task[Option[ControllerType]] =
                             ZIO.fail(new Exception("Rejected database update"))
                           def delete(id: ControllerTypeId): Task[Chunk[ControllerType]]  =
                             ZIO.fail(new Exception("Referenced controller type"))
                         }
          env         <- ControllerTypeManager
                           .live
                           .build
                           .provideSomeLayer[Scope](
                             ZLayer.succeed[ControllerTypeRepository](failing) ++ ZLayer.succeed(f.storage)
                           )
          updated     <- env
                           .get[ControllerTypeManager]
                           .update(created.copy(description = "changed"), Some(replacement))
                           .exit
          deleted     <- env.get[ControllerTypeManager].delete(created.id).exit
          after       <- f.bytes(created.id)
          stored      <- f.repository.get(created.id)
        } yield assertTrue(
          updated.isFailure,
          deleted.isFailure,
          before == after,
          stored.contains(created)
        )
      }
    },
    test("imports validated firmware under the remapped ID and configured code directory") {
      ZIO.scoped {
        for {
          f         <- fixture
          archive   <- f.stage()
          created   <- f.manager.create(data, archive)
          service   <- f.serialization
          bundle    <- service.exportControllerType(created.id).compress.runCollect
          _         <- f.manager.delete(created.id)
          _         <- service.importData(ZStream.fromChunk(bundle))
          imported  <- f.repository.list()
          firmware  <- f.bytes(imported.head.id)
          contents  <- ZStream
                         .fromChunk(firmware)
                         .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
                         .mapZIO {
                           case (entry, content) =>
                             content.runCollect.map(bytes => entry.name -> new String(bytes.toArray))
                         }
                         .runCollect
          published <- ZIO.attemptBlocking(
                         Files.isRegularFile(f.root.resolve(s"firmware/${imported.head.id}.tar.gz"))
                       )
        } yield assertTrue(
          imported.size == 1,
          imported.head.id != created.id,
          imported.head.name == created.name,
          contents == Chunk("platformio.ini" -> "[env]"),
          published
        )
      }
    },
    test("imports reject missing or corrupt firmware before creating its controller type") {
      ZIO.scoped {
        for {
          f       <- fixture
          service <- f.serialization
          entity  <- f.repository.create(data)
          _       <- f.repository.delete(entity.id)
          json     = entity.toJson.getBytes
          entry    = (
                       ArchiveEntry(s"controllerTypes/${entity.id}.json", Some(json.length.toLong)),
                       ZStream.fromIterable(json)
                     )
          missing <- service.importData(ZStream.succeed(entry).compress).exit
          corrupt  = (
                       ArchiveEntry(s"code/${entity.id}.tar.gz", Some(3L)),
                       ZStream.fromIterable("bad".getBytes)
                     )
          invalid <- service.importData(ZStream(entry, corrupt).compress).exit
          records <- f.repository.list()
        } yield assertTrue(
          missing.isFailure,
          invalid.isFailure,
          records.isEmpty,
          missing
            .causeOption
            .exists(_.failureOption.exists(_.getMessage.contains("Missing required code archive"))),
          invalid
            .causeOption
            .exists(
              _.failureOption.exists(_.getMessage.contains("entities already committed may remain"))
            )
        )
      }
    },
    test("staged names reject arbitrary paths and create commands require an archive") {
      for {
        paths  <- ZIO.foreach(List("../x.tar.gz", "/tmp/x.tar.gz", "code/1.tar.gz"))(
                    StaticService.stagedCode(_).exit
                  )
        command = s"""{"save-controller-type":{"data":${data.toJson}}}""".fromJson[Command]
      } yield assertTrue(
        paths.forall(_.isFailure),
        command.isLeft
      )
    }
  )
}
