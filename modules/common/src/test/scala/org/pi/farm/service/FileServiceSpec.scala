package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.model.Types.*
import org.pi.farm.utils.Archive.*

import zio.*
import zio.compress.{ArchiveEntry, GzipDecompressor, TarUnarchiver}
import zio.compress.Tar.*
import zio.stream.{ZSink, ZStream}
import zio.test.*

import org.apache.commons.compress.archivers.tar.TarArchiveEntry

object FileServiceSpec extends PiFarmSpec {

  private def stagingWithGlobs(
    globs: Chunk[String]
  ): ZIO[Scope, Throwable, (os.Path, FileService)] =
    for {
      root    <- ZIO.acquireRelease(
                   ZIO.attemptBlocking(os.temp.dir(prefix = "pi-farm-static-test-"))
                 )(root => ZIO.attemptBlocking(os.remove.all(root, ignoreErrors = true)).orDie)
      dir      = root / "staging"
      service <-
        FileService
          .live
          .build
          .provideSomeLayer[Scope](
            ZLayer.succeed(
              FileService.Config(
                root,
                "firmware",
                "images",
                dir,
                globs,
                3,
                16
              )
            )
          )
    } yield dir -> service.get[FileService]

  private val staging =
    stagingWithGlobs(
      Chunk("platformio.ini", "**/*.c", "**/*.cpp", "**/*.h", "**/*.hpp", "**/*.ino")
    )

  private def upload(files: (String, String)*) =
    ZStream.fromIterable(files).map {
      case (name, text) => name -> ZStream.fromIterable(text.getBytes)
    }

  private def listing(dir: os.Path): Task[Chunk[String]] =
    ZIO.attemptBlocking(
      os.list.stream(dir).map(_.last).foldLeft(Chunk.empty[String])(_ :+ _)
    )

  private def untar(archive: os.Path): Task[Map[String, String]] =
    ZStream
      .fromPath(archive.toNIO)
      .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
      .mapZIO {
        case (entry, content) => content.runCollect.map(c => entry.name -> new String(c.toArray))
      }
      .runCollect
      .map(_.toMap)

  def spec = suite("FileService.stageCode")(
    test("matches nested code globs against project-relative paths") {
      val matchers =
        Chunk("platformio.ini", "**/*.c", "**/*.cpp", "**/*.h", "**/*.hpp", "**/*.ino")
          .map(FileService.compileGlob)
      val path     = os.SubPath("Cooler/src/main.cpp")
      assertTrue(
        FileService.matchesCodeFile(path, matchers),
        !FileService.matchesCodeFile(os.SubPath("Cooler/README.md"), matchers)
      )
    },
    test("rejects invalid glob patterns when creating the service") {
      for {
        result <- ZIO.scoped(stagingWithGlobs(Chunk("["))).exit
      } yield assertTrue(result.isFailure)
    },
    test("publishes code under the configured directory") {
      for {
        (dir, service) <- staging
        _              <- ZIO.attemptBlocking {
                            os.makeDir.all(dir)
                            os.write(dir / "upload.tar.gz", "firmware")
                          }
        _              <- service.publishCode(os.SubPath("upload.tar.gz"), 123.toControllerTypeId)
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
                              "Cooler/README.md"      -> "not included",
                              "Cooler/.pio/build/x.o" -> "binary"
                            )
                          )
        files          <- untar(dir / name)
        left           <- listing(dir)
      } yield assertTrue(
        files == Map("platformio.ini" -> "[env]", "src/main.cpp" -> "int main() {}"),
        left == Chunk(name.toString)
      )
    },
    test("filters unmatched files and includes platformio.ini only when configured") {
      for {
        (dir, service)          <- staging
        name                    <- service.stageCode(
                                     upload(
                                       "Cooler/platformio.ini" -> "[env]",
                                       "Cooler/src/main.cpp"   -> "int main() {}",
                                       "Cooler/README.md"      -> "not included"
                                     )
                                   )
        files                   <- untar(dir / name)
        (_, withoutProjectGlob) <- stagingWithGlobs(Chunk("**/*.cpp"))
        rejected                <- withoutProjectGlob
                                     .stageCode(
                                       upload("Cooler/platformio.ini" -> "[env]", "Cooler/src/main.cpp" -> "code")
                                     )
                                     .exit
        configOnly              <- service.stageCode(upload("platformio.ini" -> "[env]"))
        configFiles             <- untar(dir / configOnly)
      } yield assertTrue(
        files == Map("platformio.ini" -> "[env]", "src/main.cpp" -> "int main() {}"),
        rejected.isFailure,
        configFiles == Map("platformio.ini" -> "[env]")
      )
    },
    test("prepares validated archives in staging and removes unpublished copies on scope close") {
      for {
        (dir, service) <- staging
        name           <- service.stageCode(
                            upload(
                              "platformio.ini" -> "[env]",
                              "src/main.cpp"   -> "int main() {}"
                            )
                          )
        path           <- ZIO.scoped {
                            service.prepareCode(name).tap { path =>
                              untar(path).flatMap(contents =>
                                ZIO
                                  .fail(new Exception("Wrong prepared project"))
                                  .unless(
                                    contents == Map(
                                      "platformio.ini" -> "[env]",
                                      "src/main.cpp"   -> "int main() {}"
                                    )
                                  )
                              )
                            }
                          }
        exists         <- ZIO.attemptBlocking(os.exists(path))
        original       <- ZIO.attemptBlocking(os.exists(dir / name))
      } yield assertTrue(!exists, original)
    },
    test("rejects unsafe archive entries, duplicate paths, missing layout and oversized entries") {
      for {
        (dir, service) <- staging
        _              <- ZIO.attemptBlocking(os.makeDir.all(dir))
        results        <- ZIO.foreach(
                            List(
                              List("../escape"      -> "x"),
                              List("/absolute"      -> "x"),
                              List("platformio.ini" -> "a", "platformio.ini" -> "b"),
                              List("src/main.cpp"   -> "x"),
                              List("platformio.ini" -> "x" * 17)
                            )
                          ) { files =>
                            val archive = dir / "invalid.tar.gz"
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
                              .run(ZSink.fromPath(archive.toNIO)) *>
                              ZIO.scoped(service.prepareCode(archive)).exit
                          }
      } yield assertTrue(results.forall(_.isFailure))
    },
    test("rejects symbolic and hard links in code archives") {
      for {
        (dir, service) <- staging
        _              <- ZIO.attemptBlocking(os.makeDir.all(dir))
        results        <-
          ZIO.foreach(List('1'.toByte, '2'.toByte)) { kind =>
            val linked  = new TarArchiveEntry("platformio.ini", kind)
            linked.setLinkName("/outside")
            val archive = dir / "linked.tar.gz"
            ZStream
              .succeed(
                (ArchiveEntry("platformio.ini", Some(0L)).withUnderlying(linked), ZStream.empty)
              )
              .compress
              .run(ZSink.fromPath(archive.toNIO)) *>
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
      } yield assertTrue(results.forall(_.isInstanceOf[FileService.FileException]), left.isEmpty)
    }
  )
}
