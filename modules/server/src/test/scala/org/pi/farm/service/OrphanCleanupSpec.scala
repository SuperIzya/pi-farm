package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.fake.{
  ConfigurationRepositoryFake,
  ConfigurationServiceFake,
  ControllerRepositoryFake,
  ControllerTypeRepositoryFake,
  PeripheryTypeRepositoryFake
}
import org.pi.farm.model.{Direction, PeripheryType}
import org.pi.farm.model.Types.*
import org.pi.farm.storage.PeripheryTypeRepository

import zio.*
import zio.stream.ZStream
import zio.test.*

import java.nio.file.{Files, FileVisitResult, Path, SimpleFileVisitor}
import java.nio.file.attribute.{BasicFileAttributes, FileTime}
import scala.jdk.CollectionConverters.*

object OrphanCleanupSpec extends PiFarmSpec {
  override def aspects =
    Chunk(
      TestAspect.shrinks(1),
      TestAspect.forked,
      TestAspect.samples(20),
      TestAspect.sequential,
      TestAspect.timeout(1.minute),
      TestAspect.timed
    )

  private val KeptImage   = "00000000-0000-0000-0000-000000000001.png"
  private val OrphanImage = "00000000-0000-0000-0000-000000000002.png"

  private case class Fixture(root: Path, storage: FileService, config: FileService.Config) {
    val code: Path    = root.resolve("static/code")
    val images: Path  = root.resolve("static").resolve(config.imageDir)
    val staging: Path = root.resolve("staging")

    def worker(
      controllerExists: ControllerTypeId => Task[Boolean] = _ => ZIO.succeed(false),
      imageReferenced: String => Task[Boolean] = _ => ZIO.succeed(false)
    ): ZIO[Scope, Throwable, OrphanCleanup.Worker] =
      OrphanCleanup.make(
        config,
        storage,
        controllerExists,
        imageReferenced
      )
  }

  private def removeTree(path: Path): Unit = {
    Files.walkFileTree(
      path,
      new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        override def postVisitDirectory(
          directory: Path,
          error: java.io.IOException
        ): FileVisitResult                                                              = {
          if (error != null) throw error
          Files.delete(directory)
          FileVisitResult.CONTINUE
        }
      }
    )
    ()
  }

  private def fixture(imageDir: String = "images"): ZIO[Scope, Throwable, Fixture] =
    for {
      root     <- ZIO.acquireRelease(
                    ZIO.attemptBlocking(Files.createTempDirectory("pi-farm-cleanup-test-"))
                  )(path => ZIO.attemptBlocking(removeTree(path)).orDie)
      config    = FileService.Config(
                    root.resolve("static").toString,
                    "code",
                    imageDir,
                    root.resolve("staging").toString,
                    Chunk("platformio.ini", "**/*.c", "**/*.cpp", "**/*.h", "**/*.hpp", "**/*.ino"),
                    100,
                    5_242_880
                  )
      services <- FileService
                    .live
                    .build
                    .provideSomeLayer[Scope](
                      ZLayer.succeed(config)
                    )
    } yield Fixture(root, services.get[FileService], config)

  private def write(path: Path): Task[Unit]     =
    for {
      now <- Clock.instant
      _   <- ZIO.attemptBlocking {
               Files.writeString(path, "test")
               Files.setLastModifiedTime(path, FileTime.from(now.minusSeconds(86401)))
             }
    } yield ()
  private def exists(path: Path): Task[Boolean] = ZIO.attemptBlocking(Files.exists(path))

  def spec = suite("OrphanCleanup")(
    test("configured image paths roundtrip through saving, cleanup, export and import") {
      ZIO.scoped {
        import org.pi.farm.utils.Archive.*
        fixture("assets/photos").flatMap { f =>
          (for {
            repo     <- ZIO.service[PeripheryTypeRepository]
            service  <- ZIO.service[StorageService]
            created  <- service.savePeripheryType(
                          PeripheryType.New(
                            name = "sensor".toPeripheryChannelName,
                            description = "",
                            image = "data:image/png;base64,dGVzdA==",
                            connections = NonEmptyChunk(
                              PeripheryType.Connection(
                                name = "value".toPeripheryChannelName,
                                direction = Direction.In,
                                units = "%".toUnits,
                                `type` = "Float",
                                presentation = None
                              )
                            )
                          )
                        )
            _        <- ZIO.attemptBlocking(
                          Files.setLastModifiedTime(
                            Path.of(f.config.baseDir).resolve(created.image),
                            FileTime.from(java.time.Instant.EPOCH)
                          )
                        )
            worker   <- f.worker(imageReferenced = repo.referencesImage)
            _        <- worker.scanCycle
            bytes    <- f.storage.getStaticResource(created.image).flatMap(_._2.runCollect)
            bundle   <- service.exportPeripheryType(created.id).compress.runCollect
            entries  <- ZStream.fromChunk(bundle).decompress
            _        <- service.importData(ZStream.fromChunk(bundle))
            imported <- repo.list()
            data     <- ZIO.foreach(imported)(p =>
                          f.storage.getStaticResource(p.image).flatMap(_._2.runCollect)
                        )
          } yield assertTrue(
            Path.of(created.image).getParent == Path.of("assets/photos"),
            bytes == Chunk.fromArray("test".getBytes),
            imported.size == 2,
            imported.forall(_.image.startsWith("assets/photos/")),
            entries.exists(_._1 == s"images/${created.image}"),
            data.forall(_ == bytes)
          )).provideSome[Scope](
            ZLayer.succeed[FileService](f.storage),
            ConfigurationRepositoryFake.empty,
            ConfigurationServiceFake.empty,
            ControllerRepositoryFake.empty,
            ControllerTypeRepositoryFake.empty,
            PeripheryTypeRepositoryFake.empty,
            ControllerTypeService.live,
            StorageService.live
          )
        }
      }
    },
    test("keeps references, removes orphans, and ignores non-managed files and symlinks") {
      ZIO.scoped {
        for {
          f           <- fixture()
          calls       <- Ref.make(List.empty[String])
          worker      <- f.worker(
                           id => calls.update(_ :+ s"code:${id.asInt}").as(id.asInt == 1),
                           image => calls.update(_ :+ image).as(image == s"images/$KeptImage")
                         )
          _           <- ZIO.foreachDiscard(
                           List(
                             "1.tar.gz",
                             "2.tar.gz",
                             "0.tar.gz",
                             "01.tar.gz",
                             "2147483648.tar.gz",
                             "prefix-5.tar.gz",
                             "6.tar.gz.tmp",
                             "upload.tmp"
                           )
                         )(name => write(f.code.resolve(name)))
          _           <- ZIO.foreachDiscard(List(KeptImage, OrphanImage, "notes.png"))(name =>
                           write(f.images.resolve(name))
                         )
          _           <- ZIO.attemptBlocking {
                           Files.createDirectory(f.code.resolve("3.tar.gz"))
                           Files.createSymbolicLink(f.code.resolve("4.tar.gz"), f.code.resolve("1.tar.gz"))
                           Files.createSymbolicLink(
                             f.images.resolve("00000000-0000-0000-0000-000000000003.png"),
                             f.images.resolve(KeptImage)
                           )
                           Files.writeString(f.code.resolve("7.tar.gz"), "young")
                           Files.writeString(f.code.resolve("8.tar.gz"), "boundary")
                           Files.writeString(f.code.resolve("3.tar.gz/nested"), "keep")
                         }
          now         <- Clock.instant
          _           <- ZIO.attemptBlocking {
                           Files.setLastModifiedTime(
                             f.code.resolve("3.tar.gz"),
                             FileTime.from(now.minusSeconds(86401))
                           )
                           Files.setLastModifiedTime(f.code.resolve("7.tar.gz"), FileTime.from(now))
                           Files.setLastModifiedTime(
                             f.code.resolve("8.tar.gz"),
                             FileTime.from(now.minusSeconds(86400))
                           )
                         }
          _           <- worker.scanCycle
          kept        <- exists(f.code.resolve("1.tar.gz"))
          orphan      <- exists(f.code.resolve("2.tar.gz"))
          keptImage   <- exists(f.images.resolve(KeptImage))
          orphanImage <- exists(f.images.resolve(OrphanImage))
          ignored     <- ZIO.foreach(
                           List(
                             "0.tar.gz",
                             "01.tar.gz",
                             "2147483648.tar.gz",
                             "prefix-5.tar.gz",
                             "6.tar.gz.tmp",
                             "7.tar.gz",
                             "8.tar.gz",
                             "upload.tmp",
                             "3.tar.gz",
                             "4.tar.gz"
                           )
                         )(name => exists(f.code.resolve(name)))
          lookups     <- calls.get
        } yield assertTrue(
          kept,
          !orphan,
          keptImage,
          !orphanImage,
          ignored.forall(identity),
          lookups.toSet == Set("code:1", "code:2", s"images/$KeptImage", s"images/$OrphanImage"),
          lookups.take(2).forall(_.startsWith("code:"))
        )
      }
    },
    test("database failures retain files and do not stop later checks or directories") {
      ZIO.scoped {
        for {
          f      <- fixture()
          worker <- f.worker(id =>
                      if (id.asInt == 1) ZIO.fail(new Exception("database unavailable"))
                      else ZIO.succeed(false)
                    )
          _      <- write(f.code.resolve("1.tar.gz"))
          _      <- write(f.code.resolve("2.tar.gz"))
          _      <- write(f.images.resolve(OrphanImage))
          _      <- worker.scanCycle
          code   <- exists(f.code.resolve("1.tar.gz"))
          orphan <- exists(f.code.resolve("2.tar.gz"))
          image  <- exists(f.images.resolve(OrphanImage))
        } yield assertTrue(code, !orphan, !image)
      }
    },
    test("directory failures do not stop the other directory or subsequent cycles") {
      ZIO.scoped {
        for {
          f      <- fixture()
          worker <- f.worker()
          _      <- ZIO.attemptBlocking(Files.delete(f.code))
          _      <- write(f.images.resolve(OrphanImage))
          _      <- worker.scanCycle
          image  <- exists(f.images.resolve(OrphanImage))
          _      <- ZIO.attemptBlocking(Files.createDirectory(f.code))
          _      <- write(f.code.resolve("1.tar.gz"))
          _      <- worker.scanCycle
          code   <- exists(f.code.resolve("1.tar.gz"))
        } yield assertTrue(!image, !code)
      }
    },
    test("staging expires by top-level timestamp, recursively and without following links") {
      ZIO.scoped {
        for {
          f          <- fixture()
          worker     <- f.worker()
          now        <- Clock.instant
          old         = FileTime.from(now.minusSeconds(86401))
          boundary    = FileTime.from(now.minusSeconds(86400))
          _          <- ZIO.attemptBlocking {
                          Files.createDirectory(f.staging.resolve("old-project"))
                          Files.createDirectory(f.staging.resolve("recent-project"))
                          Files.writeString(f.root.resolve("outside"), "keep")
                          Files.writeString(f.staging.resolve("old-project/fresh-child"), "fresh")
                          Files.createSymbolicLink(f.staging.resolve("old-project/link"), f.root)
                          Files.writeString(f.staging.resolve("old-file"), "old")
                          Files.writeString(f.staging.resolve("recent-file"), "keep")
                          Files.writeString(f.staging.resolve("boundary"), "keep")
                          Files.writeString(f.staging.resolve("recent-project/old-child"), "old")
                          Files.setLastModifiedTime(f.staging.resolve("old-project"), old)
                          Files.setLastModifiedTime(f.staging.resolve("old-file"), old)
                          Files.setLastModifiedTime(f.staging.resolve("recent-file"), FileTime.from(now))
                          Files.setLastModifiedTime(f.staging.resolve("boundary"), boundary)
                          Files.setLastModifiedTime(f.staging.resolve("recent-project/old-child"), old)
                          Files.setLastModifiedTime(f.staging.resolve("recent-project"), FileTime.from(now))
                        }
          _          <- worker.scanCycle
          oldProject <- exists(f.staging.resolve("old-project"))
          oldFile    <- exists(f.staging.resolve("old-file"))
          recentFile <- exists(f.staging.resolve("recent-file"))
          recent     <- exists(f.staging.resolve("recent-project/old-child"))
          exact      <- exists(f.staging.resolve("boundary"))
          outside    <- exists(f.root.resolve("outside"))
        } yield assertTrue(!oldProject, !oldFile, recentFile, recent, exact, outside)
      }
    },
    test("publication protects an unreferenced image until its database reference is committed") {
      ZIO.scoped {
        for {
          f          <- fixture()
          referenced <- Ref.make(false)
          worker     <- f.worker(imageReferenced = _ => referenced.get)
          published  <- Promise.make[Nothing, Unit]
          commit     <- Promise.make[Nothing, Unit]
          save       <- f.storage
                          .withPublicationLock(
                            write(f.images.resolve(KeptImage)) *> published.succeed(()) *>
                              commit.await *> referenced.set(true)
                          )
                          .forkScoped
          _          <- published.await
          scan       <- worker.scanCycle.forkScoped
          _          <- ZIO.yieldNow.repeatN(10)
          during     <- exists(f.images.resolve(KeptImage))
          _          <- commit.succeed(())
          _          <- save.join
          _          <- scan.join
          after      <- exists(f.images.resolve(KeptImage))
        } yield assertTrue(during, after)
      }
    },
    test("runs immediately, waits 24 hours after completion, and stops with its scope") {
      ZIO.scoped {
        for {
          f           <- fixture()
          calls       <- Ref.make(0)
          first       <- Promise.make[Nothing, Unit]
          release     <- Promise.make[Nothing, Unit]
          workerScope <- ZIO.acquireRelease(Scope.make)(_.close(Exit.unit))
          worker      <- f.worker(id =>
                           calls.updateAndGet(_ + 1).flatMap { count =>
                             if (count == 1) first.succeed(()) *> release.await.as(true)
                             else ZIO.succeed(true)
                           }
                         ).provideEnvironment(ZEnvironment(workerScope))
          _           <- write(f.code.resolve("1.tar.gz"))
          fiber       <- worker.run.forkIn(workerScope)
          _           <- first.await
          _           <- TestClock.adjust(48.hours)
          blocked     <- calls.get
          _           <- release.succeed(())
          _           <- TestClock.adjust(23.hours)
          before      <- calls.get
          _           <- TestClock.adjust(1.hour)
          _           <- calls.get.repeatUntil(_ == 2)
          _           <- workerScope.close(Exit.unit)
          result      <- fiber.poll
          _           <- TestClock.adjust(48.hours)
          after       <- calls.get
        } yield assertTrue(blocked == 1, before == 1, after == 2, result.exists(_.isInterrupted))
      }
    },
    test("scope shutdown closes an active directory stream and the low-priority executor") {
      ZIO.scoped {
        for {
          f           <- fixture()
          workerScope <- ZIO.acquireRelease(Scope.make)(_.close(Exit.unit))
          checking    <- Promise.make[Nothing, Unit]
          worker      <- f.worker(_ => checking.succeed(()) *> ZIO.never)
                           .provideEnvironment(ZEnvironment(workerScope))
          _           <- write(f.code.resolve("1.tar.gz"))
          fiber       <- worker.run.forkIn(workerScope)
          _           <- checking.await
          thread      <- ZIO.succeed(Thread.currentThread()).onExecutor(worker.executor)
          openBefore  <- directoryHandles(f.code)
          _           <- workerScope.close(Exit.unit)
          openAfter   <- directoryHandles(f.code)
          result      <- fiber.poll
        } yield assertTrue(
          thread.getName == "pi-farm-orphan-cleanup",
          thread.getPriority == Thread.MIN_PRIORITY,
          !thread.isAlive,
          openBefore > 0,
          openAfter == 0,
          result.exists(_.isInterrupted)
        )
      }
    } @@ TestAspect.ifProp("os.name")(_.equalsIgnoreCase("Linux"))
  ) @@ TestAspect.sequential

  private def directoryHandles(path: Path): Task[Int] =
    ZIO.attemptBlocking {
      val handles = Files.newDirectoryStream(Path.of("/proc/self/fd"))
      try
        handles.iterator().asScala.count { handle =>
          try Files.readSymbolicLink(handle) == path
          catch {
            case _: java.nio.file.NoSuchFileException => false
          }
        }
      finally handles.close()
    }
}
