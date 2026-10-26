package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.*
import org.pi.farm.model.ControllerType
import org.pi.farm.model.Types.*
import org.pi.farm.storage.ControllerTypeRepository

import zio.*
import zio.compress.{ArchiveEntry, GzipDecompressor, TarUnarchiver}
import zio.json.*
import zio.stream.ZStream
import zio.test.*

import java.nio.file.{Files, Path}

object ControllerTypeServiceImportSpec extends PiFarmSpec {
  import org.pi.farm.utils.Archive.*

  import StorageService.*

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

    def serialization: ZIO[Scope, Throwable, StorageService] =
      ZLayer
        .make[StorageService](
          ZLayer.succeed(storage),
          ZLayer.succeed(repository),
          ZLayer.succeed(service),
          PeripheryTypeRepositoryFake.empty,
          ControllerRepositoryFake.empty,
          ConfigurationRepositoryFake.empty,
          ConfigurationServiceFake.empty,
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
                    ZIO.foreachDiscard(paths.reverse)(path =>
                      ZIO.attemptBlocking(Files.delete(path))
                    )
                  )
                  .orDie
              )
      env  <- ZLayer
                .make[FileService & ControllerTypeRepository & ControllerTypeService](
                  FileService.live,
                  ControllerTypeRepositoryFake.empty,
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

  def spec = suite("ControllerTypeService imports")(
    test("imports validated firmware under the remapped ID and configured code directory") {
      ZIO.scoped {
        for {
          f         <- fixture
          archive   <- f.stage()
          created   <- f.service.create(data, archive)
          service   <- f.serialization
          bundle    <- service.exportControllerType(created.id).compress.runCollect
          _         <- f.service.delete(created.id)
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
          contents.toMap == Map(
            "platformio.ini" -> "[env]",
            "src/main.cpp"   -> "int main() {}"
          ),
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
    }
  )
}
