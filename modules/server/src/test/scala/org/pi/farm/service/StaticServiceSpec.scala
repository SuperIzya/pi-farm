package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.model.Types.*

import zio.*
import zio.compress.{ArchiveEntry, GzipDecompressor, TarUnarchiver}
import zio.compress.Tar.*
import zio.stream.{ZSink, ZStream}
import zio.test.*

import java.nio.file.{Files, Path}

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import StorageService.*

object StaticServiceSpec extends PiFarmSpec {

  private val staging: ZIO[Scope, Throwable, (Path, StaticService)] =
    for {
      root    <- ZIO.acquireRelease(
                   ZIO.attemptBlocking(Files.createTempDirectory("pi-farm-static-test-"))
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
      dir      = root.resolve("staging")
      service <-
        StaticService
          .live
          .build
          .provideSomeLayer[Scope](
            ZLayer.succeed(
              StaticService.Config(root.toString, "firmware", "images", dir.toString, 3, 16)
            )
          )
    } yield dir -> service.get[StaticService]

  private def upload(files: (String, String)*) =
    ZStream.fromIterable(files).map {
      case (name, text) => name -> ZStream.fromIterable(text.getBytes)
    }

  private def listing(dir: Path): Task[List[String]] =
    ZStream
      .fromJavaStream(Files.list(dir))
      .map(_.getFileName.toString)
      .runCollect
      .map(_.toList)

  private def untar(archive: Path): Task[Map[String, String]] =
    ZStream
      .fromPath(archive)
      .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
      .mapZIO {
        case (entry, content) => content.runCollect.map(c => entry.name -> new String(c.toArray))
      }
      .runCollect
      .map(_.toMap)

  def spec = suite("StaticService.stageCode")(
    test("publishes code under the configured directory") {
      for {
        (dir, service) <- staging
        _              <- ZIO.attemptBlocking {
                            Files.createDirectories(dir)
                            Files.writeString(dir.resolve("upload.tar.gz"), "firmware")
                          }
        _              <- service.publishCode(Path.of("upload.tar.gz"), "123.tar.gz")
        archive        <- service.getCodeArchive(123.toControllerTypeId)
        content        <- ZIO
                            .fromOption(archive)
                            .orElseFail(new Exception("Code archive not found"))
                            .flatMap(_._2.runCollect)
      } yield assertTrue(content == Chunk.fromArray("firmware".getBytes))
    },
    test("strips the enclosing folder, skips .pio and leaves only the archive") {
      for {
        (dir, service) <- staging
        name           <- service.stageCode(
                            upload(
                              "Cooler/platformio.ini" -> "[env]",
                              "Cooler/src/main.cpp"   -> "int main() {}",
                              "Cooler/.pio/build/x.o" -> "binary"
                            )
                          )
        files          <- untar(dir.resolve(name))
        left           <- listing(dir)
      } yield assertTrue(
        files == Map("platformio.ini" -> "[env]", "src/main.cpp" -> "int main() {}"),
        left == List(name)
      )
    },
    test("prepares validated archives in staging and removes unpublished copies on scope close") {
      for {
        (dir, service) <- staging
        name           <- service.stageCode(upload("platformio.ini" -> "[env]"))
        path           <- ZIO.scoped {
                            service.prepareCode(Path.of(name)).tap { path =>
                              untar(path).flatMap(contents =>
                                ZIO
                                  .fail(new Exception("Wrong prepared project"))
                                  .unless(contents == Map("platformio.ini" -> "[env]"))
                              )
                            }
                          }
        exists         <- ZIO.attemptBlocking(Files.exists(path))
        original       <- ZIO.attemptBlocking(Files.exists(dir.resolve(name)))
      } yield assertTrue(!exists, original)
    },
    test("rejects unsafe archive entries, duplicate paths, missing layout and oversized entries") {
      for {
        (dir, service) <- staging
        _              <- ZIO.attemptBlocking(Files.createDirectories(dir))
        results        <- ZIO.foreach(
                            List(
                              List("../escape"      -> "x"),
                              List("/absolute"      -> "x"),
                              List("platformio.ini" -> "a", "platformio.ini" -> "b"),
                              List("src/main.cpp"   -> "x"),
                              List("platformio.ini" -> "x" * 17)
                            )
                          ) { files =>
                            val archive = dir.resolve("invalid.tar.gz")
                            ZStream
                              .fromIterable(files)
                              .map {
                                case (name, text) =>
                                  (
                                    ArchiveEntry(name, Some(text.getBytes.length.toLong)),
                                    ZStream.fromIterable(text.getBytes)
                                  )
                              }
                              .compress
                              .run(ZSink.fromPath(archive)) *>
                              ZIO.scoped(service.prepareCode(archive)).exit
                          }
      } yield assertTrue(results.forall(_.isFailure))
    },
    test("rejects symbolic and hard links in code archives") {
      for {
        (dir, service) <- staging
        _              <- ZIO.attemptBlocking(Files.createDirectories(dir))
        results        <-
          ZIO.foreach(List('1'.toByte, '2'.toByte)) { kind =>
            val linked  = new TarArchiveEntry("platformio.ini", kind)
            linked.setLinkName("/outside")
            val archive = dir.resolve("linked.tar.gz")
            ZStream
              .succeed(
                (ArchiveEntry("platformio.ini", Some(0L)).withUnderlying(linked), ZStream.empty)
              )
              .compress
              .run(ZSink.fromPath(archive)) *>
              ZIO.scoped(service.prepareCode(archive)).exit
          }
      } yield assertTrue(results.forall(_.isFailure))
    },
    test("rejects invalid uploads and cleans up staging") {
      for {
        (dir, service) <- staging
        results        <- ZIO.foreach(
                            List(
                              upload("Cooler/../../evil"   -> "x"),
                              upload("/etc/passwd"         -> "x"),
                              upload("Cooler/src/main.cpp" -> "x"),
                              upload("platformio.ini"      -> "a", "platformio.ini" -> "b"),
                              upload("platformio.ini"      -> "x" * 17),
                              upload((0 to 3).map(i => s"$i.ini" -> "x")*)
                            )
                          )(files => service.stageCode(files).flip)
        left           <- listing(dir)
      } yield assertTrue(results.forall(_.isInstanceOf[StaticService.FileException]), left.isEmpty)
    }
  )
}
