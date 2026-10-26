package org.pi.farm.service

import org.pi.farm.model.ControllerType
import org.pi.farm.model.Types.*
import org.pi.farm.storage.{ControllerTypeRepository, DbSpec}

import zio.*
import zio.compress.{GzipDecompressor, TarUnarchiver}
import zio.stream.ZStream
import zio.test.*

import java.nio.file.{Files, Path}

object ControllerTypeServiceSpec extends DbSpec {
  private val data = ControllerType.New("board".toName, "", None, Map.empty, None)

  private case class Fixture(
    root: Path,
    storage: FileService,
    repository: ControllerTypeRepository,
    service: ControllerTypeService
  ) {
    def stage(text: String = "[env]"): Task[Path] =
      storage
        .stageCode(
          ZStream.succeed(
            "platformio.ini" -> ZStream.fromIterable(text.getBytes)
          ) ++ ZStream.succeed(
            "src/main.cpp" -> ZStream.fromIterable("int main() {}".getBytes)
          )
        )
        .map(name => root.resolve("staging").resolve(name))

    def bytes(id: ControllerTypeId): Task[Chunk[Byte]] =
      storage
        .getCodeArchive(id)
        .someOrFail(new Exception("Missing archive"))
        .flatMap(_._2.runCollect)
  }

  private val fixture: ZIO[Scope, Throwable, Fixture] =
    for {
      root <- ZIO.acquireRelease(
                ZIO.attemptBlocking(Files.createTempDirectory("pi-farm-controller-type-"))
              )(root =>
                ZStream
                  .fromJavaStream(Files.walk(root))
                  .runCollect
                  .flatMap(paths =>
                    ZIO.foreachDiscard(paths.reverse)(path =>
                      ZIO.attemptBlocking(Files.delete(path))
                    )
                  )
                  .orDie
              )
      env  <- ZLayer
                .make[FileService & ControllerTypeRepository & ControllerTypeService](
                  FileService.live,
                  controllerTypeRepositoryLayer,
                  ControllerTypeService.live,
                  ZLayer.succeed(
                    FileService.Config(
                      root.toString,
                      "firmware",
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
                      3,
                      64
                    )
                  )
                )
                .build
    } yield Fixture(
      root,
      env.get[FileService],
      env.get[ControllerTypeRepository],
      env.get[ControllerTypeService]
    )

  def spec = suite("ControllerTypeService")(
    test("creates with firmware and preserves it on metadata-only updates") {
      ZIO.scoped {
        for {
          f       <- fixture
          archive <- f.stage()
          created <- f.service.create(data, archive)
          before  <- f.bytes(created.id)
          updated <- f.service.update(created.copy(description = "changed"))
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
          invalid <- f.service.create(data, f.root.resolve("bad.tar.gz")).exit
          missing <- f.service.create(data, f.root.resolve("missing.tar.gz")).exit
          records <- f.repository.list()
        } yield assertTrue(invalid.isFailure, missing.isFailure, records.isEmpty)
      }
    },
    test("failed validation leaves metadata and existing firmware unchanged") {
      ZIO.scoped {
        for {
          f       <- fixture
          archive <- f.stage()
          created <- f.service.create(data, archive)
          before  <- f.bytes(created.id)
          result  <-
            f.service
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
          created      <- f.service.create(data, archive)
          first        <- f.stage("[env:first]")
          second       <- f.stage("[env:second]")
          firstUpdate  <- f.service.update(created, Some(first))
          firstBytes   <- f.bytes(created.id)
          secondUpdate <- f.service.update(created, Some(second))
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
          contents.toMap == Map(
            "platformio.ini" -> "[env:second]",
            "src/main.cpp"   -> "int main() {}"
          )
        )
      }
    },
    test("publication failure is explicit, retains the record, and a later upload repairs it") {
      ZIO.scoped {
        for {
          f        <- fixture
          archive  <- f.stage()
          _        <- ZIO.attemptBlocking(Files.writeString(f.root.resolve("firmware"), "blocker"))
          result   <- f.service.create(data, archive).exit
          records  <- f.repository.list()
          _        <- ZIO.attemptBlocking(Files.delete(f.root.resolve("firmware")))
          repaired <- f.service.update(records.head, Some(archive))
          bytes    <- f.bytes(records.head.id)
        } yield assertTrue(result.isFailure, records.size == 1, repaired.isDefined, bytes.nonEmpty)
      }
    },
    test("deletes the record and its archive") {
      ZIO.scoped {
        for {
          f         <- fixture
          archive   <- f.stage()
          created   <- f.service.create(data, archive)
          remaining <- f.service.delete(created.id)
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
          remaining <- f.service.delete(created.id)
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
          created     <- f.service.create(data, archive)
          before      <- f.bytes(created.id)
          replacement <- f.stage("[env:replacement]")
          failing      = new ControllerTypeRepository {
                           export f.repository.{create, exists, get, list}
                           def update(data: ControllerType): Task[Option[ControllerType]] =
                             ZIO.fail(new Exception("Rejected database update"))
                           def delete(id: ControllerTypeId): Task[Chunk[ControllerType]]  =
                             ZIO.fail(new Exception("Referenced controller type"))
                         }
          env         <- ControllerTypeService
                           .live
                           .build
                           .provideSomeLayer[Scope](
                             ZLayer.succeed[ControllerTypeRepository](failing) ++
                               ZLayer.succeed(f.storage)
                           )
          updated     <- env
                           .get[ControllerTypeService]
                           .update(created.copy(description = "changed"), Some(replacement))
                           .exit
          deleted     <- env.get[ControllerTypeService].delete(created.id).exit
          after       <- f.bytes(created.id)
          stored      <- f.repository.get(created.id)
        } yield assertTrue(
          updated.isFailure,
          deleted.isFailure,
          before == after,
          stored.contains(created)
        )
      }
    }
  )
}
