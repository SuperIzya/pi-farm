package org.pi.farm.service

import org.pi.farm.PiFarmSpec
import org.pi.farm.model.FirmwareRequest.Action
import org.pi.farm.model.Process.{Output, Phase, Status}
import org.pi.farm.model.Types.*
import org.pi.farm.service.FirmwareService.*

import zio.*
import zio.process.{Command, Process}
import zio.stream.ZStream
import zio.test.*

import java.io.ByteArrayOutputStream
import java.nio.file.{Files, Path}
import java.util.zip.GZIPOutputStream
import scala.jdk.CollectionConverters.*

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}

object FirmwareServiceSpec extends PiFarmSpec {
  private val Id          = 7.toControllerId
  private val TypeId      = 3.toControllerTypeId
  private val ConfigValue = FirmwareService.Config("test-network", "test-password", 1.minute)

  private case class Fixture(root: Path, files: FileService.Config, storage: FileService) {
    def manager(task: Task[Execution]): ZIO[Scope, Throwable, FirmwareService] =
      manager((_, _, _, _) => task)
    def manager(
      run: Runner,
      config: FirmwareService.Config = ConfigValue,
      lookup: ControllerId => Task[ControllerTypeId] = _ => ZIO.succeed(TypeId)
    ): ZIO[Scope, Throwable, FirmwareService]                                  =
      FirmwareService.make(config, files, storage, lookup, 9090, run)

    def publish(contents: String = "[env:test]"): Task[Unit] =
      for {
        name <- storage.stageCode(
                  ZStream.fromIterable(
                    List(
                      "platformio.ini" -> ZStream.fromIterable(contents.getBytes),
                      "src/main.cpp"   -> ZStream.fromIterable("int main() {}".getBytes)
                    )
                  )
                )
        _    <- storage.withPublicationLock(storage.publishCode(Path.of(name), TypeId))
      } yield ()
  }

  private def fixture: ZIO[Scope, Throwable, Fixture] =
    for {
      root <- ZIO.acquireRelease(
                ZIO.attemptBlocking(Files.createTempDirectory("pi-farm-firmware-test-"))
              )(path =>
                ZIO.attemptBlocking {
                  val entries = Files.walk(path)
                  try entries.iterator().asScala.toList.reverse.foreach(Files.delete)
                  finally entries.close()
                }.orDie
              )
      files = FileService.Config(
                root.resolve("static").toString,
                "code",
                "images",
                root.resolve("staging").toString,
                Chunk("platformio.ini", "**/*.c", "**/*.cpp", "**/*.h", "**/*.hpp", "**/*.ino"),
                10,
                4096
              )
      env  <- FileService
                .live
                .build
                .provideSomeLayer[Scope](
                  ZLayer.succeed(files)
                )
      f     = Fixture(root, files, env.get[FileService])
      _    <- f.publish()
    } yield f

  private val success: Runner = (_, _, _, _) => ZIO.succeed(Execution.empty)

  def spec = suite("FirmwareService")(
    suite("Parallel tests")(
      test("starts idle; rejects concurrent starts immediately; await observes completion") {
        ZIO.scoped {
          for {
            f       <- fixture
            entered <- Promise.make[Nothing, Unit]
            finish  <- Promise.make[Nothing, Unit]
            manager <- f.manager(entered.succeed(()) *> finish.await.unit.as(Execution.empty))
            idle    <- manager.await
            _       <- manager.start(Id, Action.Build)
            _       <- entered.await
            second  <- manager.start(Id, Action.Build).exit
            waiting <- manager.await.forkScoped
            pending <- waiting.poll
            _       <- finish.succeed(())
            result  <- waiting.join
            status  <- manager.status
          } yield assertTrue(
            idle.phase == Phase.Idle,
            second == Exit.fail(Busy),
            pending.isEmpty,
            result == Status(Phase.Succeeded, Some(Id), Some(0)),
            status == result
          )
        }
      },
      test("nonzero exit and launch failures release the slot for explicit retry") {
        ZIO.scoped {
          for {
            f       <- fixture
            calls   <- Ref.make(0)
            manager <- f.manager((_, _, _, _) =>
                         calls.updateAndGet(_ + 1).flatMap {
                           case 1 => ZIO.succeed(Execution.empty)
                           case 2 => ZIO.fail(new Exception("launch failed"))
                           case _ => ZIO.succeed(Execution.empty)
                         }
                       )
            _       <- manager.start(Id, Action.Build)
            launch  <- manager.await
            _       <- manager.start(Id, Action.Build)
            failed  <- manager.await
            _       <- manager.start(Id, Action.Build)
            retried <- manager.await
            count   <- calls.get
          } yield assertTrue(
            failed.phase == Phase.Failed,
            failed.exitCode.contains(1),
            failed.error.exists(_.contains("launch failed")),
            launch.phase == Phase.Succeeded,
            retried.phase == Phase.Succeeded,
            count == 3
          )
        }
      },
      test("preparation failures release the slot") {
        ZIO.scoped {
          for {
            f       <- fixture
            calls   <- Ref.make(0)
            manager <- f.manager(
                         success,
                         lookup = _ =>
                           calls.updateAndGet(_ + 1).flatMap {
                             case 1 => ZIO.fail(new Exception("controller missing"))
                             case _ => ZIO.succeed(TypeId)
                           }
                       )
            _       <- manager.start(Id, Action.Build)
            failed  <- manager.await
            _       <- manager.start(Id, Action.Build)
            result  <- manager.await
          } yield assertTrue(failed.phase == Phase.Failed, result.phase == Phase.Succeeded)
        }
      },
      test("timeout stops work and permits retry") {
        ZIO.scoped {
          for {
            f       <- fixture
            entered <- Promise.make[Nothing, Unit]
            stopped <- Ref.make(false)
            calls   <- Ref.make(0)
            manager <- f.manager(
                         (_, _, _, _) =>
                           calls.updateAndGet(_ + 1).flatMap {
                             case 1 =>
                               entered
                                 .succeed(()) *> ZIO.yieldNow *> ZIO
                                 .never
                                 .ensuring(stopped.set(true))
                             case _ => ZIO.succeed(Execution.empty)
                           },
                         ConfigValue.copy(timeout = 1.second)
                       )
            _       <- manager.start(Id, Action.Build)
            _       <- entered.await
            _       <- zio.test.Live.live(ZIO.sleep(10.millis))
            _       <- TestClock.adjust(2.seconds)
            result  <- manager.await
            ended   <- stopped.get
            _       <- manager.start(Id, Action.Build)
            retried <- manager.await
          } yield assertTrue(
            ended,
            result.phase == Phase.Timeout,
            retried.phase == Phase.Succeeded
          )
        }
      },
      test("application scope shutdown stops the active operation") {
        ZIO.scopeWith { parent =>
          for {
            f       <- fixture
            scope   <- parent.fork
            entered <- Promise.make[Nothing, Unit]
            stopped <- Ref.make(false)
            manager <-
              scope.extend(
                f.manager(
                  entered.succeed(()).forever.ensuring(stopped.set(true)).as(Execution.empty)
                )
              )
            _       <- manager.start(Id, Action.Build)
            _       <- entered.await
            _       <- zio.test.Live.live(ZIO.sleep(10.millis))
            _       <- scope.close(Exit.unit)
            ended   <- stopped.get
            result  <- manager.await
          } yield assertTrue(ended, result.phase == Phase.Canceled)
        }
      },
      test("recreates one workspace for every operation, replacement and controller") {
        ZIO.scoped {
          for {
            f       <- fixture
            paths   <- Ref.make(List.empty[Path])
            cached  <- Ref.make(List.empty[Boolean])
            manager <- f.manager((_, path, _, _) =>
                         ZIO
                           .attemptBlocking(Files.exists(path.resolve("cached")))
                           .flatMap(exists => cached.update(_ :+ exists)) *>
                           paths.update(_ :+ path).as(Execution.empty)
                       )
            _       <- manager.start(Id, Action.Build)
            _       <- manager.await
            first   <- paths.get.map(_.head)
            _       <- ZIO.attemptBlocking(Files.writeString(first.resolve("cached"), "keep"))
            _       <- manager.start(Id, Action.Build)
            _       <- manager.await
            _       <- f.publish("[env:replacement]")
            _       <- manager.start(Id, Action.Build)
            _       <- manager.await
            _       <- manager.start(8.toControllerId, Action.Build)
            _       <- manager.await
            all     <- paths.get
            stale   <- cached.get
            source  <- ZIO.attemptBlocking(Files.readString(all.last.resolve("platformio.ini")))
          } yield assertTrue(
            all(0) == all(1),
            all.distinct.size == 1,
            stale == List(false, false, false, false),
            source == "[env:replacement]",
            all.forall(_.getParent == Path.of(f.files.stagingDir))
          )
        }
      },
      test(
        "recent workspace survives cleanup, stays fresh while running, and expires after completion"
      ) {
        ZIO.scoped {
          for {
            f       <- fixture
            entered <- Promise.make[Nothing, Path]
            release <- Promise.make[Nothing, Unit]
            manager <-
              f.manager(
                (_, path, _, _) => entered.succeed(path) *> release.await.as(Execution.empty),
                ConfigValue.copy(timeout = 48.hours)
              )
            worker  <- OrphanCleanup.make(
                         f.files,
                         f.storage,
                         _ => ZIO.succeed(true),
                         _ => ZIO.succeed(false)
                       )
            _       <- manager.start(Id, Action.Build)
            path    <- entered.await
            _       <- worker.scanCycle
            kept    <- ZIO.attemptBlocking(Files.exists(path.resolve("platformio.ini")))
            _       <- TestClock.adjust(25.hours)
            now     <- Clock.instant
            touched <- ZIO.attemptBlocking(Files.getLastModifiedTime(path).toInstant)
            _       <- worker.scanCycle
            active  <- ZIO.attemptBlocking(Files.exists(path.resolve("platformio.ini")))
            _       <- release.succeed(())
            result  <- manager.await
            _       <- TestClock.adjust(25.hours)
            _       <- worker.scanCycle
            removed <- ZIO.attemptBlocking(!Files.exists(path))
          } yield assertTrue(
            kept,
            !touched.isBefore(now.minusSeconds(3600)),
            active,
            result.phase == Phase.Succeeded,
            removed
          )
        }
      },
      test("passes arguments, all four variables and local artifact directories") {
        ZIO.scoped {
          for {
            f                <- fixture
            seen             <- Ref.make(Option.empty[(Command, Path, Map[String, String])])
            manager          <- f.manager((args, path, env, _) =>
                                  seen.set(Some((args, path, env))).as(Execution.empty)
                                )
            _                <- manager.start(Id, Action.Upload, Some("/dev/ttyUSB0"))
            result           <- manager.await
            values           <- seen.get.someOrFail(new Exception("process not invoked"))
            (args, path, env) = values
          } yield assertTrue(
            result.phase == Phase.Succeeded,
            args == Command("pio", "run", "--target", "upload", "--upload-port", "/dev/ttyUSB0"),
            env("CONTROLLER_ID") == "7",
            env("UDP_PORT") == "9090",
            env("WIFI_SSID") == "test-network",
            env("WIFI_PASSWORD") == "test-password",
            env
              .filterNot {
                case (key, _) => Set("CONTROLLER_ID", "UDP_PORT", "WIFI_SSID", "WIFI_PASSWORD")(key)
              }
              .values
              .forall(value => Path.of(value).startsWith(path))
          )
        }
      },
      test("rejects ambiguous USB detection, accepts one device or an explicit port") {
        ZIO.scoped {
          inline def isCommand(cmd: Command)(expected: NonEmptyChunk[String]) = cmd match {
            case Command.Standard(args, _, _, _, _, _, _) => args == expected
            case _                                        => false
          }
          for {
            f       <- fixture
            devices <- Ref.make("""[{"port":"/dev/a"},{"port":"/dev/b"}]""")
            calls   <- Ref.make(List.empty[Command])
            manager <- f.manager((cmd, _, _, emit) =>
                         calls.update(_ :+ cmd) *> (cmd match {
                           case Command.Standard(args, _, _, _, _, _, _)
                               if args.contains("--json-output") =>
                             devices.get.flatMap(text => emit(Output.stdout(text)))
                           case _ => ZIO.unit
                         }).as(Execution.empty)
                       )
            _       <- manager.start(Id, Action.Upload)
            failed  <- manager.await
            _       <- devices.set("""[{"port":"/dev/a","description":"board", "hwid": "1234"}]""")
            _       <- manager.start(Id, Action.Upload)
            result  <- manager.await
            args    <- calls.get
            _        = println(args)
          } yield assertTrue(
            isCommand(args.last)(
              NonEmptyChunk("pio", "run", "--target", "upload", "--upload-port", "/dev/a")
            ),
            failed.phase == Phase.Failed,
            result.phase == Phase.Succeeded,
            args.count {
              case Command.Standard(args, _, _, _, _, _, _) if args.contains("--json-output") =>
                true
              case _                                                                          => false
            } == 2
          )
        }
      },
      test("output is live, drains without subscribers, and does not replay") {
        ZIO.scoped {
          for {
            f       <- fixture
            count   <- Ref.make(0)
            manager <- f.manager((_, _, _, emit) =>
                         ZIO.foreachDiscard(1 to 1000)(i =>
                           ZIO
                             .runtime
                             .flatMap(
                               _.run(
                                 emit(
                                   (if (i % 2 == 0) Output.stdout else Output.stderr) (i.toString)
                                 )
                               )
                             )
                             .provide(Runtime.removeDefaultLoggers)
                         ) *>
                           count.update(_ + 1).as(Execution.empty)
                       )
            _       <- manager.start(Id, Action.Build)
            _       <- manager.await
            stream  <- manager.output
            before  <- stream.take(1).runCollect.forkScoped
            pending <- before.poll
            _       <- manager.start(Id, Action.Build)
            first   <- before.join
            _       <- manager.await
            ran     <- count.get
          } yield assertTrue(pending.isEmpty, first.nonEmpty, ran == 2)
        }
      },
      test("failed extraction is not reused; traversal and links are rejected") {
        ZIO.scoped {
          for {
            f       <- fixture
            manager <- f.manager(success)
            results <- ZIO.foreach(List("../outside", "/absolute", "link")) { name =>
                         for {
                           archive <- ZIO.attemptBlocking {
                                        val bytes = new ByteArrayOutputStream()
                                        val tar   =
                                          new TarArchiveOutputStream(new GZIPOutputStream(bytes))
                                        val entry = if (name == "link") {
                                          val link = new TarArchiveEntry(name, '2'.toByte)
                                          link.setLinkName("../outside")
                                          link
                                        } else new TarArchiveEntry(name)
                                        entry.setSize(0)
                                        tar.putArchiveEntry(entry)
                                        tar.closeArchiveEntry()
                                        tar.close()
                                        bytes.toByteArray
                                      }
                           _       <-
                             ZIO.attemptBlocking(
                               Files.write(
                                 Path
                                   .of(f.files.baseDir)
                                   .resolve("code")
                                   .resolve(s"$TypeId.tar.gz"),
                                 archive
                               )
                             )
                           _       <- manager.start(Id, Action.Build)
                           result  <- manager.await
                         } yield result.phase
                       }
            _       <- f.publish()
            _       <- manager.start(Id, Action.Build)
            retried <- manager.await
            outside <- ZIO.attemptBlocking(Files.exists(f.root.resolve("outside")))
          } yield assertTrue(
            results.forall(_ == Phase.Failed),
            retried.phase == Phase.Succeeded,
            !outside
          )
        }
      }
    ) @@ TestAspect.parallel,
    suite("Sequential tests")(
      test("cancel waits for cleanup, retains protection until stopped, and allows retry") {
        ZIO.scoped {
          for {
            f          <- fixture
            entered    <- Promise.make[Nothing, Path]
            stopping   <- Promise.make[Nothing, Unit]
            release    <- Promise.make[Nothing, Unit]
            calls      <- Ref.make(0)
            manager    <- f.manager(
                            (_, path, _, _) =>
                              calls.updateAndGet(_ + 1).flatMap {
                                case 1 =>
                                  (entered.succeed(path) *> ZIO.never)
                                    .ensuring(
                                      ZIO.uninterruptible(stopping.succeed(()) *> release.await)
                                    )
                                case _ => ZIO.succeed(Execution.empty)
                              },
                            ConfigValue
                          )
            _          <- manager.start(Id, Action.Build)
            path       <- entered.await
            worker     <- OrphanCleanup.make(
                            f.files,
                            f.storage,
                            _ => ZIO.succeed(true),
                            _ => ZIO.succeed(false)
                          )
            cancel     <- manager.cancel.forkScoped
            _          <- stopping.await
            _          <- worker.scanCycle
            protected_ <- ZIO.attemptBlocking(Files.exists(path.resolve("platformio.ini")))
            busy       <- manager.start(Id, Action.Build).exit
            _          <- release.succeed(())
            _          <- cancel.join
            result     <- manager.await
            _          <- worker.scanCycle
            recent     <- ZIO.attemptBlocking(Files.exists(path.resolve("platformio.ini")))
            _          <- manager.start(Id, Action.Build)
            retried    <- manager.await
            runCount   <- calls.get
          } yield assertTrue(
            protected_,
            busy.isFailure,
            result.phase == Phase.Canceled,
            recent,
            retried.phase == Phase.Succeeded,
            runCount == 2
          )
        }
      },
      test("real process drains both streams in order and propagates exit code") {
        ZIO.scoped {
          for {
            f         <- fixture
            lines     <- Ref.make(Option.empty[Output])
            execution <- FirmwareService.run(
                           Command(
                             "/bin/sh",
                             "-c",
                             "i=0; while [ \"$i\" -lt 3000 ]; do printf '%s\\n' \"$i\"; printf '%s\\n' \"$i\" >&2; i=$((i+1)); done; exit 4"
                           ),
                           f.root,
                           Map.empty,
                           line => lines.set(Some(line))
                         )

            code <- execution.waitFor
            line <- lines.get
          } yield assertTrue(
            code == ExitCode(4),
            line.exists(_.message == "2999")
          )
        }
      },
      test("real process cancellation terminates the process and child") {
        ZIO.scoped {
          for {
            f         <- fixture
            pids      <- Promise.make[Nothing, List[Long]]
            execution <- FirmwareService
                           .run(
                             Command(
                               "/bin/sh",
                               "-c",
                               "sleep 20 & child=$!; printf '%s %s\\n' \"$$\" \"$child\"; wait \"$child\""
                             ),
                             f.root,
                             Map.empty,
                             line => pids.succeed(line.message.split(" ").map(_.toLong).toList).unit
                           )
            ids       <- pids.await
            _         <- execution.interrupt
            alive     <- ZIO.attemptBlocking(
                           ids.exists(pid =>
                             ProcessHandle.of(pid).isPresent && ProcessHandle.of(pid).get().isAlive
                           )
                         )
          } yield assertTrue(!alive)
        }
      }
    ) @@ TestAspect.sequential
  ) @@ TestAspect.sequential
}
