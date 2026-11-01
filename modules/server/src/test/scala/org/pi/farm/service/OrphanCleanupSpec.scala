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

  private case class Fixture(root: os.Path, storage: FileService, config: FileService.Config) {
    val code: os.Path    = root / "static" / "code"
    val images: os.Path  = root / "static" / config.imageDir
    val staging: os.Path = root / "staging"

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

  private def removeTree(path: os.Path): Unit = {
    os.remove.all(path)
  }

  private def fixture(imageDir: String = "images"): ZIO[Scope, Throwable, Fixture] =
    for {
      root     <- ZIO.acquireRelease(
                    ZIO.attemptBlocking(os.temp.dir(prefix = "pi-farm-cleanup-test-"))
                  )(path => ZIO.attemptBlocking(removeTree(path)).orDie)
      config    = FileService.Config(
                    root / "static",
                    "code",
                    os.SubPath(imageDir),
                    root / "staging",
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

  private def write(path: os.Path): Task[Unit]     =
    for {
      now <- Clock.instant
      time = now.minusSeconds(86401).toEpochMilli()
      _   <- ZIO.attemptBlocking {
               os.write.over(path, "test")
               os.mtime.set(path, time)
             }
    } yield ()
  private def exists(path: os.Path): Task[Boolean] = ZIO.attemptBlocking(os.exists(path))

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
                          os.mtime
                            .set(
                              f.root / "static" / os.SubPath(created.image),
                              java.time.Instant.EPOCH.toEpochMilli()
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
            os.SubPath(created.image) / os.up == os.SubPath("assets") / "photos",
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
                         )(name => write(f.code / name))
          _           <- ZIO.foreachDiscard(List(KeptImage, OrphanImage, "notes.png"))(name =>
                           write(f.images / name)
                         )
          _           <- ZIO.attemptBlocking {
                           os.makeDir.all(f.code / "3.tar.gz")
                           os.symlink(f.code / "4.tar.gz", f.code / "1.tar.gz")
                           os.symlink(
                             f.images / "00000000-0000-0000-0000-000000000003.png",
                             f.images / KeptImage
                           )
                           os.write(f.code / "7.tar.gz", "young")
                           os.write(f.code / "8.tar.gz", "boundary")
                           os.write(f.code / "3.tar.gz" / "nested", "keep")
                         }
          now         <- Clock.instant
          _           <- ZIO.attemptBlocking {
                           os.mtime
                             .set(
                               f.code / "3.tar.gz",
                               now.minusSeconds(86401).toEpochMilli()
                             )
                           os.mtime.set(f.code / "7.tar.gz", now.toEpochMilli())
                           os.mtime
                             .set(
                               f.code / "8.tar.gz",
                               now.minusSeconds(86400).toEpochMilli()
                             )
                         }
          _           <- worker.scanCycle
          kept        <- exists(f.code / "1.tar.gz")
          orphan      <- exists(f.code / "2.tar.gz")
          keptImage   <- exists(f.images / KeptImage)
          orphanImage <- exists(f.images / OrphanImage)
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
                         )(name => exists(f.code / name))
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
          _      <- write(f.code / "1.tar.gz")
          _      <- write(f.code / "2.tar.gz")
          _      <- write(f.images / OrphanImage)
          _      <- worker.scanCycle
          code   <- exists(f.code / "1.tar.gz")
          orphan <- exists(f.code / "2.tar.gz")
          image  <- exists(f.images / OrphanImage)
        } yield assertTrue(code, !orphan, !image)
      }
    },
    test("directory failures do not stop the other directory or subsequent cycles") {
      ZIO.scoped {
        for {
          f      <- fixture()
          worker <- f.worker()
          _      <- ZIO.attemptBlocking(os.remove.all(f.code))
          _      <- write(f.images / OrphanImage)
          _      <- worker.scanCycle
          image  <- exists(f.images / OrphanImage)
          _      <- ZIO.attemptBlocking(os.makeDir.all(f.code))
          _      <- write(f.code / "1.tar.gz")
          _      <- worker.scanCycle
          code   <- exists(f.code / "1.tar.gz")
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
                          os.makeDir(f.staging / "old-project")
                          os.makeDir(f.staging / "recent-project")
                          os.write(f.root / "outside", "keep")
                          os.write(f.staging / "old-project/fresh-child", "fresh")
                          os.symlink(f.staging / "old-project/link", f.root)
                          os.write(f.staging / "old-file", "old")
                          os.write(f.staging / "recent-file", "keep")
                          os.write(f.staging / "boundary", "keep")
                          os.write(f.staging / "recent-project/old-child", "old")
                          os.mtime.set(f.staging / "old-project", old.toMillis())
                          os.mtime.set(f.staging / "old-file", old.toMillis())
                          os.mtime.set(f.staging / "recent-file", now.toEpochMilli())
                          os.mtime.set(f.staging / "boundary", boundary.toMillis())
                          os.mtime.set(f.staging / "recent-project/old-child", old.toMillis())
                          os.mtime.set(f.staging / "recent-project", now.toEpochMilli())
                        }
          _          <- worker.scanCycle
          oldProject <- exists(f.staging / "old-project")
          oldFile    <- exists(f.staging / "old-file")
          recentFile <- exists(f.staging / "recent-file")
          recent     <- exists(f.staging / "recent-project/old-child")
          exact      <- exists(f.staging / "boundary")
          outside    <- exists(f.root / "outside")
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
                            write(f.images / KeptImage) *> published.succeed(()) *>
                              commit.await *> referenced.set(true)
                          )
                          .forkScoped
          _          <- published.await
          scan       <- worker.scanCycle.forkScoped
          _          <- ZIO.yieldNow.repeatN(10)
          during     <- exists(f.images / KeptImage)
          _          <- commit.succeed(())
          _          <- save.join
          _          <- scan.join
          after      <- exists(f.images / KeptImage)
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
          _           <- write(f.code / "1.tar.gz")
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
          _           <- ZIO.foreachDiscard(1 to 4)(id => write(f.code / s"$id.tar.gz"))
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

  private def directoryHandles(path: os.Path): Task[Long] =
    ZIO.attemptBlocking {
      import FileService.toStream
      os.list
        .stream(os.Path("/proc/self/fd"))
        .toStream
        .filterZIO(handle => ZIO.attempt(os.readLink(handle) == path).orElseSucceed(false))
        .runCount
    }.flatten
}
