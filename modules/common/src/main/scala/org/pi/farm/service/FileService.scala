package org.pi.farm.service
import org.pi.farm.model.Types.ControllerTypeId
import org.pi.farm.utils.Archive.*
import org.pi.farm.utils.ConfigCompanion

import zio.*
import zio.compress.{ArchiveEntry, GzipDecompressor, TarUnarchiver}
import zio.compress.Tar.*
import zio.config.magnolia.DeriveConfig
import zio.stream.{ZSink, ZStream}

import java.util.Base64
import scala.util.matching.Regex

import org.apache.commons.compress.archivers.tar.TarArchiveEntry

trait FileService {
  def getStaticResource(path: os.SubPath): Task[(Int, ZStream[Any, Throwable, Byte])]
  def getStaticResource(path: String): Task[(Int, ZStream[Any, Throwable, Byte])] =
    ZIO
      .attempt(os.SubPath(path))
      .mapError(e => FileService.FileException(s"Invalid resource path: $path", e))
      .flatMap(getStaticResource)
  def getCodeArchive(id: ControllerTypeId): Task[Option[(Int, ZStream[Any, Throwable, Byte])]]

  def touch(path: os.Path): Task[Unit]
  def publishImage(path: os.Path, name: String): Task[os.SubPath]
  def publishCode(path: os.SubPath | os.Path, id: ControllerTypeId): Task[os.SubPath]
  private[service] def prepareCode(path: os.SubPath | os.Path): ZIO[Scope, Throwable, os.Path]
  private[service] def unpackCode(id: ControllerTypeId): ZIO[Scope, Throwable, os.Path]
  def deleteCode(id: ControllerTypeId): Task[Unit]
  def saveImage(content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, os.SubPath]
  def saveImage(content: Chunk[Byte]): ZIO[Any, Throwable, os.SubPath] =
    saveImage(ZStream.fromChunk(content))
  def saveImage(base64: String): ZIO[Any, Throwable, os.SubPath]       =
    ZIO
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
  ): Task[os.SubPath]

  def stagingDir: os.Path
}

object FileService {
  val ExcludedDirs: Set[String] = Set(".pio")

  def stagedCode(name: String): Task[os.SubPath] =
    ZIO
      .fail(FileException("Expected a staged <uuid>.tar.gz archive name"))
      .unless(
        name.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.tar\\.gz$")
      )
      .as(os.SubPath(name))

  /** @param codeFileGlobs
    *   glob patterns for files retained in uploaded projects, including `platformio.ini`
    * @param maxCodeFiles
    *   maximum number of files in an uploaded code project (excluded directories not counted)
    * @param maxCodeFileSize
    *   maximum size in bytes of each uploaded code project file
    */
  case class Config(
    baseDir: os.Path,
    codeDir: os.SubPath,
    imageDir: os.SubPath,
    stagingDir: os.Path,
    codeFileGlobs: Chunk[String],
    maxCodeFiles: Int,
    maxCodeFileSize: Long
  )
  given DeriveConfig[os.Path]    = DeriveConfig[String].mapAttempt(os.Path(_, os.pwd))
  given DeriveConfig[os.SubPath] = DeriveConfig[String].mapAttempt(os.SubPath(_))
  object Config extends ConfigCompanion[Config]("file-service")

  def live: RLayer[Config, FileService] = ZLayer {
    for {
      config <- ZIO.service[Config]
      lock   <- Semaphore.make(1)
    } yield new Live(config, lock)
  }

  private final class Live(config: Config, lock: Semaphore) extends FileService {

    private val baseDir         = config.baseDir
    private val imagesRelDir    = config.imageDir
    private val imagesDir       = baseDir / imagesRelDir
    private val codeRelDir      = config.codeDir
    private val codeDir         = baseDir / codeRelDir
    val stagingDir              = config.stagingDir
    private val codeDestination = stagingDir / "firmware"
    private val maximum         = config.maxCodeFiles.toLong * (config.maxCodeFileSize + 2048) + 1048576

    private val codeMatchers = config.codeFileGlobs.map(compileGlob)

    def withPublicationLock[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
      lock.withPermit(effect)

    def getStaticResource(path: os.SubPath): Task[(Int, ZStream[Any, Throwable, Byte])] =
      ZIO
        .attempt(baseDir / path)
        .flatMap { fullPath =>
          fail(s"Resource outside static directory: $path")
            .unless(fullPath.startsWith(baseDir)) *>
            ZIO.attempt {
              if (os.exists(fullPath)) {
                val data = ZStream.fromPath(fullPath.toNIO)
                (os.size(fullPath).toInt, data)
              } else {
                val data = ZStream.fromResource(path.toString)
                (os.size(fullPath).toInt, data)
              }
            }
        }
        .orElseFail(FileException(s"Resource not found: $path"))

    def getCodeArchive(
      id: ControllerTypeId
    ): Task[Option[(Int, ZStream[Any, Throwable, Byte])]] =
      staticResourceIfExists(codeRelDir / s"$id.tar.gz")

    def touch(path: os.Path): Task[Unit] =
      for {
        _   <- fail(s"$path does not exist").unlessZIO(ZIO.attemptBlocking(os.exists(path)))
        now <- Clock.instant
        _   <- ZIO.attemptBlocking(os.mtime.set(path, now.toEpochMilli))
      } yield ()

    private def staticResourceIfExists(
      path: os.SubPath
    ): Task[Option[(Int, ZStream[Any, Throwable, Byte])]] =
      ZIO
        .attempt(baseDir / path)
        .flatMap { fullPath =>
          fail(s"Resource outside static directory: $path")
            .unless(fullPath.startsWith(baseDir)) *>
            ZIO.attemptBlocking {
              Option.when(os.isFile(fullPath)) {
                (os.size(fullPath).toInt, ZStream.fromPath(fullPath.toNIO))
              }
            }
        }

    def saveImage(content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, os.SubPath] =
      for {
        newName <- Random.nextUUID.map(uuid => imagesRelDir / s"$uuid.png")
        newFile  = baseDir / newName
        _       <- ZIO.attemptBlocking(os.makeDir.all(newFile / os.up))
        size    <- content.run(ZSink.fromPath(newFile.toNIO))
        _       <- ZIO.logInfo(s"Saved image to $newName with size $size bytes")
      } yield newName

    def publishImage(path: os.Path, name: String): Task[os.SubPath] =
      ZIO.attemptBlocking {
        val target = imagesDir / name
        os.makeDir.all(target / os.up)
        os.move(path, target, replaceExisting = true)
        target.subRelativeTo(baseDir)
      }

    def stageCode(
      files: ZStream[Any, Throwable, (String, ZStream[Any, Throwable, Byte])]
    ): Task[os.SubPath] =
      ZIO.scoped {
        for {
          uuid   <- Random.nextUUID
          work   <-
            ZIO.acquireRelease(
              ZIO.attemptBlocking {
                val dir = stagingDir / uuid.toString
                os.makeDir.all(dir)
                dir
              }
            )(dir => deleteTree(dir).catchAllCause(ZIO.logErrorCause(s"Failed to delete $dir", _)))
          _      <-
            files.runFoldZIO(0) {
              case (count, (name, content)) =>
                projectPath(name).flatMap { path =>
                  if (path.segments.exists(p => ExcludedDirs(p.toString)))
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
                                           val target = work / path
                                           os.makeDir.all(target / os.up)
                                           os.write(target, Array.emptyByteArray)
                                           target
                                         }
                                         .mapError(e => FileException(s"Failed to write $name", e))
                            written <- content
                                         .take(config.maxCodeFileSize + 1)
                                         .run(ZSink.fromPath(target.toNIO))
                            _       <- fail(s"$name is larger than ${config.maxCodeFileSize} bytes")
                                         .when(written > config.maxCodeFileSize)
                          } yield count + 1
                    } yield result
                }
            }
          root   <- projectRoot(work)
          archive = stagingDir / s"$uuid.tar.gz"
          _      <- ZIO
                      .attemptBlocking(os.walk(root).filter(os.isFile(_)))
                      .flatMap { files =>
                        ZStream
                          .fromIterable(files)
                          .mapZIO { file =>
                            ZIO
                              .attemptBlocking(os.size(file))
                              .map { size =>
                                (
                                  ArchiveEntry(file.relativeTo(root).toString, Some(size)),
                                  ZStream.fromPath(file.toNIO)
                                )
                              }
                          }
                          .compress
                          .run(ZSink.fromPath(archive.toNIO))
                      }
                      .onError(_ =>
                        ZIO.attemptBlocking(os.remove(archive, checkExists = true)).ignoreLogged
                      )
          _      <- ZIO.logInfo(s"Staged code archive $archive")
        } yield archive.subRelativeTo(stagingDir)
      }

    def publishCode(path: os.SubPath | os.Path, id: ControllerTypeId): Task[os.SubPath] =
      fail(s"Invalid path: $path").whenZIO(ZIO.attempt {
        path match {
          case p: os.Path if p.startsWith(stagingDir) => false
          case p: os.SubPath                          => false
          case _                                      => true
        }
      }) *>
        ZIO.attemptBlocking {
          val target = codeDir / s"$id.tar.gz"
          val source = path match {
            case p: os.Path if p.startsWith(stagingDir) => p
            case p: os.SubPath                          => stagingDir / p
            case _                                      => throw FileException(s"Invalid path: $path")
          }
          os.move(
            source,
            target,
            atomicMove = true,
            replaceExisting = true,
            createFolders = true
          )
          target.subRelativeTo(codeDir)
        }

    def deleteCode(id: ControllerTypeId): Task[Unit] =
      ZIO.attemptBlocking(os.remove(codeDir / s"$id.tar.gz", checkExists = true)).unit

    private[service] def prepareCode(path: os.SubPath | os.Path): ZIO[Scope, Throwable, os.Path] =
      ZIO.acquireRelease {
        for {
          source <- path match {
                      case p: os.Path if p.startsWith(stagingDir) || p.startsWith(codeDir) =>
                        ZIO.succeed(p)
                      case p: os.SubPath                                                   => stagedCode(p.toString).map(stagingDir / _)
                      case _                                                               => fail(s"Invalid path: $path")
                    }
          _      <- fail(s"Code archive not found: $path")
                      .unlessZIO(
                        ZIO.attemptBlocking(os.isFile(source, false))
                      )
          _      <- fail(s"Code archive exceeds $maximum bytes")
                      .whenZIO(ZIO.attemptBlocking(os.size(source) > maximum))
          name   <- stageCode(
                      ZStream
                        .fromPath(source.toNIO)
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
        } yield stagingDir / name
      }(archive =>
        ZIO
          .attemptBlocking(os.remove(archive, checkExists = false))
          .tapErrorCause(ZIO.logErrorCause(s"Failed to remove prepared code $archive", _))
          .orDie
      )

    def unpackCode(id: ControllerTypeId): ZIO[Scope, Throwable, os.Path] =
      ZIO.scoped {
        for {
          _        <- ZIO.attemptBlocking {
                        os.makeDir.all(stagingDir)
                      }
          _        <- fail("Firmware staging directory must not be a symlink")
                        .whenZIO(ZIO.attemptBlocking(os.isLink(stagingDir)))
          prepared <- prepareCode(codeDir / s"$id.tar.gz")
          work     <- ZIO.acquireRelease(
                        ZIO.attemptBlocking(os.temp.dir(prefix = "firmware-extract-"))
                      )(path =>
                        deleteTree(path)
                          .tapErrorCause(
                            ZIO.logErrorCause(s"Failed to remove extraction directory $path", _)
                          )
                          .orDie
                      )
          _        <- ZStream
                        .fromPath(prepared.toNIO)
                        .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
                        .runForeach {
                          case (entry, content) =>
                            for {
                              relative <- projectPath(entry.name)
                              target    = work / relative
                              _        <- ZIO.attemptBlocking(os.makeDir.all(target / os.up))
                              _        <- content.run(ZSink.fromPath(target.toNIO))
                            } yield ()
                        }
          _        <- deleteTree(codeDestination)
          _        <- ZIO.attemptBlocking(
                        os.move(work, codeDestination, replaceExisting = true)
                      )
          now      <- Clock.instant
          _        <- ZIO.attemptBlocking(
                        os.mtime.set(codeDestination, now.toEpochMilli)
                      )
        } yield codeDestination
      }

    private def matchesCodeFile(path: os.SubPath): Boolean =
      FileService.matchesCodeFile(path, codeMatchers)

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
  private val RegexMeta   = "\\.^$|?*+()[]{}"

  private[service] def matchesCodeFile(path: os.SubPath, matchers: Chunk[Regex]): Boolean = {
    val segments   = path.segments.map(_.toString)
    val candidates = Iterator(segments.mkString("/")) ++
      Option.when(segments.size > 1)(segments.drop(1).mkString("/"))
    candidates.exists(candidate => matchers.exists(_.matches(candidate)))
  }

  private[service] def compileGlob(glob: String): Regex = {
    val expression = new StringBuilder("^")
    var index      = 0
    while (index < glob.length) {
      glob.charAt(index) match {
        case '*' if index + 1 < glob.length && glob.charAt(index + 1) == '*' =>
          index += 1
          if (index + 1 < glob.length && glob.charAt(index + 1) == '/') {
            index += 1
            expression.append("(?:.*/)?")
          } else expression.append(".*")
        case '*'                                                             => expression.append("[^/]*")
        case '?'                                                             => expression.append("[^/]")
        case '['                                                             =>
          val end            = glob.indexOf(']', index + 1)
          if (end < 0) throw new IllegalArgumentException(s"Invalid code file glob: $glob")
          val characterClass = glob.substring(index + 1, end)
          expression.append('[')
          if (characterClass.startsWith("!")) expression.append('^').append(characterClass.drop(1))
          else expression.append(characterClass)
          expression.append(']')
          index = end
        case char                                                            =>
          if (RegexMeta.contains(char)) expression.append('\\')
          expression.append(char)
      }
      index += 1
    }
    expression.append('$').toString.r
  }

  private inline def fail(message: String) = ZIO.fail(FileException(message))

  private def projectPath(name: String): Task[os.SubPath] =
    ZIO
      .attempt(os.SubPath(name))
      .mapError(e => FileException(s"Invalid project path: $name", e))

  private def projectRoot(work: os.Path): Task[os.Path] =
    for {
      hasProjectFile <- ZIO.attemptBlocking(os.isFile(work / ProjectFile))
      root           <-
        (if (hasProjectFile) ZIO.succeed(work)
         else
           ZIO
             .attemptBlocking {
               os.list
                 .stream(work)
                 .take(2)
                 .toList
             }
             .flatMap(paths =>
               ZIO.attemptBlocking {
                 paths match {
                   case single :: Nil if os.isDir(single) => single
                   case _                                 => work
                 }
               }
             ))
      hasProjectFile <- ZIO.attemptBlocking(os.isFile(root / ProjectFile))
      _              <- fail(s"$ProjectFile not found").unless(hasProjectFile)
    } yield root

  extension [A](gen: geny.Generator[A]) {
    def toStream: ZStream[Any, Throwable, A] =
      ZStream.unwrapScoped {
        for {
          queue   <- Queue.bounded[Either[Throwable, Option[A]]](2)
          runtime <- ZIO.runtime[Any]
          _       <- ZIO
                       .attemptBlocking {
                         gen.foreach { elem =>
                           Unsafe.unsafe { u ?=>
                             runtime.unsafe.run(queue.offer(Right(Some(elem))))
                           }
                         }
                       }
                       .foldZIO(
                         error => queue.offer(Left(error)).unit,
                         _ => queue.offer(Right(None)).unit
                       )
                       .forkScoped
        } yield ZStream
          .fromQueueWithShutdown(queue)
          .takeWhile {
            case Right(Some(_)) => true
            case _              => false
          }
          .mapZIO {
            case Right(Some(value)) => ZIO.succeed(value)
            case Left(error)        => ZIO.fail(error)
            case Right(None)        => ZIO.fail(new IllegalStateException("Unexpected end marker"))
          }
      }
  }

  private def deleteTree(dir: os.Path): Task[Unit] =
    ZIO.attemptBlocking(os.remove.all(dir, false))

  case class FileException(message: String, cause: Option[Throwable])
      extends Exception(message, cause.orNull)
  object FileException {
    def apply(message: String, cause: Throwable): FileException =
      new FileException(message, Option(cause))

    def apply(message: String): FileException = new FileException(message, None)
  }

}
