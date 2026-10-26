package org.pi.farm.service
import org.pi.farm.model.Types.*
import org.pi.farm.storage.{ControllerTypeRepository, PeripheryTypeRepository}

import zio.*
import zio.stream.ZStream

import java.nio.file.{Files, LinkOption, Path}
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.Executors

object OrphanCleanup {
  private val CodeName  = "([1-9][0-9]*)\\.tar\\.gz".r
  private val ImageName =
    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png".r

  val live: RLayer[
    FileService.Config & FileService & ControllerTypeRepository & PeripheryTypeRepository & Scope,
    Unit
  ] = ZLayer {
    for {
      config      <- ZIO.service[FileService.Config]
      storage     <- ZIO.service[FileService]
      controllers <- ZIO.service[ControllerTypeRepository]
      peripheries <- ZIO.service[PeripheryTypeRepository]
      worker      <- make(
                       config,
                       storage,
                       controllers.exists,
                       peripheries.referencesImage
                     )
      _           <- worker.run.forkScoped
    } yield ()
  }

  private[service] def make(
    config: FileService.Config,
    storage: FileService,
    controllerExists: ControllerTypeId => Task[Boolean],
    imageReferenced: String => Task[Boolean]
  ): ZIO[Scope, Throwable, Worker] =
    for {
      pool          <- ZIO.acquireRelease(ZIO.attempt {
                         Executors.newSingleThreadExecutor((task: Runnable) => {
                           val thread = new Thread(task, "pi-farm-orphan-cleanup")
                           thread.setPriority(Thread.MIN_PRIORITY)
                           thread.setDaemon(true)
                           thread
                         })
                       })(pool =>
                         ZIO
                           .attemptBlocking(pool.close())
                           .tapErrorCause(ZIO.logErrorCause("Failed to close orphan cleanup executor", _))
                           .orDie
                       )
      given Executor = Executor.fromJavaExecutor(pool)
      cleanupCode    = CleanupTask(
                         Path.of(config.baseDir).resolve(config.codeDir),
                         file =>
                           file.getFileName.toString match {
                             case CodeName(id) =>
                               id.toIntOption.fold[Task[Boolean]](ZIO.succeed(true)) { value =>
                                 controllerExists(value.toControllerTypeId)
                               }
                             case _            => ZIO.succeed(true)
                           }
                       )
      cleanupImage   = CleanupTask(
                         Path.of(config.baseDir).resolve(config.imageDir),
                         file =>
                           file.getFileName.toString match {
                             case ImageName() =>
                               imageReferenced(
                                 Path.of(config.imageDir).resolve(file.getFileName).toString
                               )
                             case _           => ZIO.succeed(true)
                           }
                       )
      cleanupStale   = CleanupTask(
                         Path.of(config.stagingDir),
                         isReferenced = _ => ZIO.succeed(false),
                         recursive = true
                       )
      worker         = new Worker(
                         storage,
                         cleanupCode,
                         cleanupImage,
                         cleanupStale
                       )
      _             <- ZIO.foreachDiscard(
                         List(
                           Path.of(config.baseDir).resolve(config.codeDir),
                           Path.of(config.baseDir).resolve(config.imageDir),
                           Path.of(config.stagingDir)
                         )
                       ) { directory =>
                         filesystem(Files.createDirectories(directory)) *>
                           ZIO
                             .fail(
                               new IllegalArgumentException(
                                 s"Cleanup directory must not be a symlink: $directory"
                               )
                             )
                             .whenZIO(filesystem(Files.isSymbolicLink(directory)))
                       }
    } yield worker

  // attemptBlocking would escape to ZIO's shared blocking pool.
  private inline def filesystem[A](operation: => A)(using executor: Executor): Task[A] =
    ZIO.attempt(operation).onExecutor(executor)

  private def report(label: String)(effect: Task[Any]): UIO[Unit] =
    effect.catchAllCause { cause =>
      if (cause.isInterruptedOnly) ZIO.interrupt
      else ZIO.logErrorCause(s"Orphan cleanup failed: $label", cause)
    }.unit

  private final case class CleanupTask(
    path: Path,
    isReferenced: Path => Task[Boolean],
    recursive: Boolean = false
  )

  private[service] final class Worker private[OrphanCleanup] (
    storage: FileService,
    tasks: CleanupTask*
  )(using private[service] val executor: Executor) {
    import OrphanCleanup.*

    private def attributes(path: Path): Task[BasicFileAttributes] =
      filesystem(
        Files.readAttributes(path, classOf[BasicFileAttributes], LinkOption.NOFOLLOW_LINKS)
      )

    private def entries(directory: Path): ZStream[Any, Throwable, Path] =
      ZStream
        .acquireReleaseWith(
          ZIO
            .fail(
              new IllegalArgumentException(
                s"Cleanup directory must not be a symlink: $directory"
              )
            )
            .whenZIO(filesystem(Files.isSymbolicLink(directory))) *>
            filesystem(Files.newDirectoryStream(directory))
        ) { stream =>
          report(s"closing $directory")(filesystem(stream.close()))
        }
        .flatMap { stream =>
          ZStream.unwrap(filesystem(stream.iterator()).map { iterator =>
            ZStream.unfoldZIO(iterator) { current =>
              filesystem {
                if (current.hasNext) Some(current.next() -> current) else None
              }
            }
          })
        }

    private def scan[A](directory: Path)(
      process: Path => Task[A]
    ): UIO[Unit] =
      report(s"scanning $directory") {
        entries(directory).mapZIO(path => report(path.toString)(process(path))).runDrain
      }

    private def removeTree(path: Path): Task[Unit] =
      for {
        attrs <- attributes(path)
        _     <- entries(path).mapZIO(removeTree).runDrain.when(attrs.isDirectory)
        _     <- filesystem(Files.delete(path))
      } yield ()

    private[service] def scanCycle: UIO[Unit] =
      Clock.instant.flatMap { now =>
        val cutoff = now.minusSeconds(24 * 60 * 60)
        ZIO.foreachDiscard(tasks) { task =>
          scan(task.path) { path =>
            storage.withPublicationLock {
              for {
                attrs <- attributes(path)
                _     <- (if (task.recursive) removeTree(path)
                          else filesystem(Files.deleteIfExists(path)).unit)
                           .unlessZIO(task.isReferenced(path))
                           .when(
                             !attrs.isSymbolicLink &&
                               (task.recursive || attrs.isRegularFile) &&
                               attrs.lastModifiedTime().toInstant.isBefore(cutoff)
                           )
              } yield ()
            }
          }
        }
      }

    private[service] def run: UIO[Unit] =
      scanCycle.repeat(Schedule.spaced(24.hours)).unit
  }
}
