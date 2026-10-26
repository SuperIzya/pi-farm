package org.pi.farm.service
import org.pi.farm.model.Types.ControllerTypeId
import org.pi.farm.utils.Archive.*
import org.pi.farm.utils.ConfigCompanion

import zio.*
import zio.compress.{ArchiveEntry, GzipDecompressor, TarUnarchiver}
import zio.compress.Tar.*
import zio.stream.{ZSink, ZStream}

import java.nio.file.{
  FileAlreadyExistsException,
  Files,
  FileSystems,
  LinkOption,
  NotDirectoryException,
  Path,
  StandardCopyOption
}
import java.nio.file.attribute.FileTime
import java.util.Base64
import scala.jdk.CollectionConverters.*

import org.apache.commons.compress.archivers.tar.TarArchiveEntry

trait FileService {
  def getStaticResource(path: Path): Task[(Int, ZStream[Any, Throwable, Byte])]
  def getStaticResource(path: String): Task[(Int, ZStream[Any, Throwable, Byte])] =
    getStaticResource(Path.of(path))
  def getCodeArchive(
    id: ControllerTypeId
  ): Task[Option[(Int, ZStream[Any, Throwable, Byte])]]

  def publishImage(path: Path, name: String): Task[Path]
  def publishCode(path: Path, id: ControllerTypeId): Task[Path]
  def prepareCode(path: Path): ZIO[Scope, Throwable, Path]
  def unpackCode(id: ControllerTypeId): ZIO[Scope, Throwable, Path]
  def deleteCode(id: ControllerTypeId): Task[Unit]
  def saveImage(content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String]
  def saveImage(content: Chunk[Byte]): ZIO[Any, Throwable, String] =
    saveImage(ZStream.fromChunk(content))
  def saveImage(base64: String): ZIO[Any, Throwable, String]       = ZIO
    .attempt {
      val bytes = Base64.getDecoder.decode(base64)
      Chunk.fromArray(bytes)
    }
    .flatMap(content => saveImage(ZStream.fromChunk(content)))

  def withPublicationLock[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A]

  /** Packs uploaded project files into a new `<uuid>.tar.gz` in the staging directory.
    *
    * @param files
    *   `(relative path, content)` pairs, consumed sequentially. A single enclosing directory (as
    *   sent by browser folder uploads) is stripped so that `platformio.ini` is at the archive root.
    *   Only files matching the configured code-file globs are retained. Files under
    *   [[FileService.ExcludedDirs]] are skipped.
    * @return
    *   archive file name, relative to the staging directory.
    * @note
    *   Fails with `IllegalArgumentException` for invalid uploads.
    */
  def stageCode(
    files: ZStream[Any, Throwable, (String, ZStream[Any, Throwable, Byte])]
  ): Task[String]
}

object FileService {
  val ExcludedDirs: Set[String] = Set(".pio")

  def stagedCode(name: String): Task[Path] =
    ZIO
      .fail(FileException("Expected a staged <uuid>.tar.gz archive name"))
      .unless(
        name.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.tar\\.gz")
      )
      .as(Path.of(name))

  /** @param codeFileGlobs
    *   glob patterns for files retained in uploaded projects, including `platformio.ini`
    * @param maxCodeFiles
    *   maximum number of files in an uploaded code project (excluded directories not counted)
    * @param maxCodeFileSize
    *   maximum size in bytes of each uploaded code project file
    */
  case class Config(
    baseDir: String,
    codeDir: String,
    imageDir: String,
    stagingDir: String,
    codeFileGlobs: Chunk[String],
    maxCodeFiles: Int,
    maxCodeFileSize: Long
  )
  object Config extends ConfigCompanion[Config]("file-service")

  def live: RLayer[Config, FileService] = ZLayer {
    for {
      config <- ZIO.service[Config]
      lock   <- Semaphore.make(1)
    } yield new Live(config, lock)
  }

  private final class Live(config: Config, lock: Semaphore) extends FileService {

    private val baseDir      = Path.of(config.baseDir)
    private val imagesRelDir = Path.of(config.imageDir)
    private val imagesDir    = baseDir.resolve(imagesRelDir)
    private val codeRelDir   = Path.of(config.codeDir)
    private val codeDir      = baseDir.resolve(codeRelDir)
    private val stagingDir   = Path.of(config.stagingDir)
    private val maximum      = config.maxCodeFiles.toLong * (config.maxCodeFileSize + 2048) + 1048576
    private val codeMatchers =
      config.codeFileGlobs.map(glob => FileSystems.getDefault.getPathMatcher(s"glob:$glob"))

    def withPublicationLock[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
      lock.withPermit(effect)

    def getStaticResource(path: Path): Task[(Int, ZStream[Any, Throwable, Byte])] =
      ZIO
        .attempt(baseDir.resolve(path).toFile.getCanonicalFile.toPath)
        .flatMap { fullPath =>
          fail(s"Resource outside static directory: $path")
            .unless(fullPath.startsWith(baseDir.toFile.getCanonicalFile.toPath)) *>
            ZIO.attempt {
              val file = fullPath.toFile()

              if (file.exists()) {
                val data = ZStream.fromPath(fullPath)
                (file.length.toInt, data)
              } else {
                val data = ZStream.fromResource(path.toString)
                (0, data)
              }
            }
        }
        .orElseFail(FileException(s"Resource not found: $path"))

    def getCodeArchive(
      id: ControllerTypeId
    ): Task[Option[(Int, ZStream[Any, Throwable, Byte])]] =
      staticResourceIfExists(codeRelDir.resolve(s"$id.tar.gz"))

    private def staticResourceIfExists(
      path: Path
    ): Task[Option[(Int, ZStream[Any, Throwable, Byte])]] =
      ZIO
        .attempt(baseDir.resolve(path).toFile.getCanonicalFile.toPath)
        .flatMap { fullPath =>
          fail(s"Resource outside static directory: $path")
            .unless(fullPath.startsWith(baseDir.toFile.getCanonicalFile.toPath)) *>
            ZIO.attemptBlocking {
              Option.when(Files.isRegularFile(fullPath)) {
                (Files.size(fullPath).toInt, ZStream.fromPath(fullPath))
              }
            }
        }

    def saveImage(content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String] =
      for {
        newName <- Random.nextUUID.map(uuid => imagesRelDir.resolve(s"$uuid.png"))
        newFile  = baseDir.resolve(newName).toFile
        _       <- ZIO.attemptBlocking(Files.createDirectories(newFile.toPath.getParent))
        size    <- content.run(ZSink.fromFile(newFile))
        _       <- ZIO.logInfo(s"Saved image to $newName with size $size bytes")
      } yield newName.toString()

    def publishImage(path: Path, name: String): Task[Path] =
      ZIO.attemptBlocking {
        val target = imagesDir.resolve(name)
        Files.createDirectories(target.getParent)
        Files.move(stagingDir.resolve(path), target)
        baseDir.relativize(target)
      }

    def stageCode(
      files: ZStream[Any, Throwable, (String, ZStream[Any, Throwable, Byte])]
    ): Task[String] =
      ZIO.scoped {
        for {
          uuid   <- Random.nextUUID
          work   <-
            ZIO.acquireRelease(
              ZIO.attemptBlocking(Files.createDirectories(stagingDir.resolve(uuid.toString)))
            )(dir => deleteTree(dir).catchAllCause(ZIO.logErrorCause(s"Failed to delete $dir", _)))
          _      <-
            files.runFoldZIO(0) {
              case (count, (name, content)) =>
                projectPath(name).flatMap { path =>
                  if (path.iterator.asScala.exists(p => ExcludedDirs(p.toString)))
                    content.runDrain.as(count)
                  else
                    for {
                      _      <- fail(s"More than ${config.maxCodeFiles} files")
                                  .when(count >= config.maxCodeFiles)
                      result <-
                        if (!matchesCodeFile(path))
                          discard(content, name).as(count + 1)
                        else
                          for {
                            target  <- ZIO
                                         .attemptBlocking {
                                           val target = work.resolve(path)
                                           Files.createDirectories(target.getParent)
                                           Files.createFile(target)
                                         }
                                         .mapError {
                                           case e @ (_: FileAlreadyExistsException |
                                               _: NotDirectoryException) =>
                                             FileException(s"Conflicting path: $name", e)
                                           case e: Throwable => e
                                         }
                            written <-
                              content.take(config.maxCodeFileSize + 1).run(ZSink.fromPath(target))
                            _       <-
                              fail(s"$name is larger than ${config.maxCodeFileSize} bytes")
                                .when(written > config.maxCodeFileSize)
                          } yield count + 1
                    } yield result
                }
            }
          root   <- projectRoot(work)
          archive = stagingDir.resolve(s"$uuid.tar.gz")
          _      <-
            ZStream
              .fromJavaStream(Files.walk(root))
              .filter(Files.isRegularFile(_))
              .mapZIO { file =>
                ZIO.attemptBlocking(Files.size(file)).map { size =>
                  (ArchiveEntry(root.relativize(file).toString, Some(size)), ZStream.fromPath(file))
                }
              }
              .compress
              .run(ZSink.fromPath(archive))
              .onError(_ => ZIO.attemptBlocking(Files.deleteIfExists(archive)).ignoreLogged)
          _      <- ZIO.logInfo(s"Staged code archive $archive")
        } yield archive.getFileName.toString
      }

    def publishCode(path: Path, id: ControllerTypeId): Task[Path] =
      ZIO.attemptBlocking {
        val target = codeDir.resolve(s"$id.tar.gz")
        Files.createDirectories(codeDir)
        Files.move(
          stagingDir.resolve(path),
          target,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING
        )
        codeDir.relativize(target)
      }

    def deleteCode(id: ControllerTypeId): Task[Unit] =
      ZIO.attemptBlocking(Files.deleteIfExists(codeDir.resolve(s"$id.tar.gz"))).unit

    def prepareCode(path: Path): ZIO[Scope, Throwable, Path] =
      ZIO.acquireRelease {
        for {
          source <- if (path.isAbsolute) ZIO.succeed(path)
                    else stagedCode(path.toString).map(stagingDir.resolve)
          _      <- fail(s"Code archive not found: $path")
                      .unlessZIO(
                        ZIO.attemptBlocking(Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS))
                      )
          _      <- fail(s"Code archive exceeds $maximum bytes")
                      .whenZIO(ZIO.attemptBlocking(Files.size(source) > maximum))
          name   <- stageCode(
                      ZStream
                        .fromPath(source)
                        .via(GzipDecompressor.decompress)
                        .chunks
                        .mapAccumZIO(0L) { (size, bytes) =>
                          val total = size + bytes.size
                          fail(s"Decompressed code archive exceeds $maximum bytes")
                            .when(total > maximum)
                            .as(total -> bytes)
                        }
                        .flattenChunks
                        .via(TarUnarchiver.unarchive)
                        .zipWithIndex
                        .mapZIO {
                          case ((entry, content), index) =>
                            val underlying = entry.underlying[TarArchiveEntry]
                            fail(s"Invalid code archive entry: ${entry.name}")
                              .when(
                                index >= config.maxCodeFiles.toLong * 4 + 16 ||
                                  (!underlying.isFile && !underlying.isDirectory) ||
                                  underlying.isSymbolicLink || underlying.isLink ||
                                  entry.uncompressedSize.exists(_ > config.maxCodeFileSize)
                              ) *>
                              (if (entry.isDirectory)
                                 projectPath(entry.name.stripSuffix("/")) *> content.runDrain.as(None)
                               else ZIO.succeed(Some(entry.name -> content)))
                        }
                        .collectSome
                    )
        } yield stagingDir.resolve(name)
      }(archive =>
        ZIO
          .attemptBlocking(Files.deleteIfExists(archive))
          .tapErrorCause(ZIO.logErrorCause(s"Failed to remove prepared code $archive", _))
          .orDie
      )

    def unpackCode(id: ControllerTypeId): ZIO[Scope, Throwable, Path] =
      ZIO.scoped {
        val destination = stagingDir.toAbsolutePath.normalize.resolve("firmware")
        for {
          _        <- ZIO.attemptBlocking {
                        Files.createDirectories(stagingDir)
                        require(
                          !Files.isSymbolicLink(stagingDir),
                          "Firmware staging directory must not be a symlink"
                        )
                      }
          prepared <- prepareCode(codeDir.resolve(s"$id.tar.gz").toAbsolutePath)
          work     <- ZIO.acquireRelease(
                        ZIO.attemptBlocking(
                          Files.createTempDirectory(destination.getParent, "firmware-extract-")
                        )
                      )(path =>
                        deleteTree(path)
                          .whenZIO(ZIO.attemptBlocking(Files.exists(path, LinkOption.NOFOLLOW_LINKS)))
                          .tapErrorCause(
                            ZIO.logErrorCause(s"Failed to remove extraction directory $path", _)
                          )
                          .orDie
                      )
          _        <- ZStream
                        .fromPath(prepared)
                        .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
                        .runForeach {
                          case (entry, content) =>
                            for {
                              relative <- projectPath(entry.name)
                              target    = work.resolve(relative)
                              _        <- ZIO.attemptBlocking(Files.createDirectories(target.getParent))
                              _        <- content.run(ZSink.fromPath(target))
                            } yield ()
                        }
          _        <- deleteTree(destination)
                        .whenZIO(
                          ZIO.attemptBlocking(Files.exists(destination, LinkOption.NOFOLLOW_LINKS))
                        )
          _        <- ZIO.attemptBlocking(
                        Files.move(work, destination, StandardCopyOption.ATOMIC_MOVE)
                      )
          now      <- Clock.instant
          _        <- ZIO.attemptBlocking(
                        Files.setLastModifiedTime(destination, FileTime.from(now))
                      )
        } yield destination
      }

    private def matchesCodeFile(path: Path): Boolean = {
      val candidates =
        Iterator(path) ++ Option.when(path.getNameCount > 1)(path.subpath(1, path.getNameCount))
      candidates.exists(candidate => codeMatchers.exists(_.matches(candidate)))
    }

    private def discard(content: ZStream[Any, Throwable, Byte], name: String): Task[Unit] =
      content
        .take(config.maxCodeFileSize + 1)
        .runCount
        .flatMap(size =>
          fail(s"$name is larger than ${config.maxCodeFileSize} bytes")
            .when(size > config.maxCodeFileSize)
            .unit
        )
  }

  private val ProjectFile = "platformio.ini"
  private val dotSegments = Set("", ".", "..")

  private inline def fail(message: String) = ZIO.fail(FileException(message))

  private def projectPath(name: String): Task[Path] =
    ZIO.attempt(Path.of(name)).flatMap { path =>
      fail(s"Invalid project path: $name")
        .when(
          path.isAbsolute || path.iterator.asScala.exists(p => dotSegments(p.toString))
        )
        .as(path)
    }

  private def projectRoot(work: Path): Task[Path] =
    for {
      hasProjectFile <- ZIO.attemptBlocking(Files.exists(work.resolve(ProjectFile)))
      root           <-
        (if (hasProjectFile) ZIO.succeed(work)
         else
           ZStream
             .fromJavaStream(Files.list(work))
             .take(2)
             .runCollect
             .flatMap(paths =>
               ZIO.attemptBlocking {
                 paths.toList match {
                   case single :: Nil if Files.isDirectory(single) => single
                   case _                                          => work
                 }
               }
             ))
      hasProjectFile <- ZIO.attemptBlocking(Files.isRegularFile(root.resolve(ProjectFile)))
      _              <- fail(s"$ProjectFile not found").unless(hasProjectFile)
    } yield root

  private def deleteTree(dir: Path): Task[Unit] =
    ZStream
      .fromJavaStream(Files.walk(dir))
      .runCollect
      .flatMap(paths =>
        ZIO.foreachDiscard(paths.reverse)(path => ZIO.attemptBlocking(Files.delete(path)))
      )

  case class FileException(message: String, cause: Option[Throwable])
      extends Exception(message, cause.orNull)
  object FileException {
    def apply(message: String, cause: Throwable): FileException =
      new FileException(message, Option(cause))

    def apply(message: String): FileException = new FileException(message, None)
  }

}

object Foo {

  val str = "123"

  str.toInt

  case class C(i: Int, j: Int)

  str
    .toIntOption // Some(123)
    .map(i => (1 to i).map(j => C(i, j)))
    .map(_.sortBy { c => (c.i, c.j) })
    .map(_.filter(_ => true))
    .flatMap(_.headOption)

}
