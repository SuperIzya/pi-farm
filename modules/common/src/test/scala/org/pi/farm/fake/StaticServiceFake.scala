package org.pi.farm.fake

import org.pi.farm.model.Types.ControllerTypeId
import org.pi.farm.service.FileService

import zio.*
import zio.stream.*

final class StaticServiceFake(
  data: Ref[Map[os.SubPath, Chunk[Byte]]],
  lock: Semaphore,
  root: Ref[os.Path]
) extends FileService {

  def stagingDir: os.Path = os.pwd / "staging"

  def publishCode(path: os.Path | os.SubPath, id: ControllerTypeId): Task[os.SubPath] =
    path match {
      case p: os.Path if p.startsWith(stagingDir) =>
        val target = os.SubPath("code") / s"$id.tar.gz"
        ZIO
          .attemptBlocking(os.read.bytes(p))
          .flatMap(bytes => data.update(_ + (target -> Chunk.fromArray(bytes))).as(target))
      case _                                      => publish(path, os.SubPath("code") / s"$id.tar.gz")
    }

  def unpackCode(id: ControllerTypeId): ZIO[Scope, Throwable, os.Path] =
    ZIO.fail(new UnsupportedOperationException("StaticServiceFake does not extract firmware"))

  def withPublicationLock[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
    lock.withPermit(effect)

  def reset: UIO[Unit] = data.set(Map.empty)

  def getStaticResource(path: os.SubPath): Task[(Int, ZStream[Any, Throwable, Byte])] =
    data
      .get
      .map {
        case map if map.contains(path) =>
          val data = map(path)
          (data.length, ZStream.fromChunk(data))
        case _                         => (0, ZStream.empty)
      }

  def getCodeArchive(
    id: ControllerTypeId
  ): Task[Option[(Int, ZStream[Any, Throwable, Byte])]] =
    data
      .get
      .map(
        _.get(os.SubPath("code") / s"$id.tar.gz")
          .map(content => (content.length, ZStream.fromChunk(content)))
      )

  def addResource(path: os.SubPath, content: Chunk[Byte]): UIO[Unit] =
    data.update(_ + (path -> content))

  def publishImage(path: os.Path | os.SubPath, name: String): Task[os.SubPath] =
    publish(path, os.SubPath("images") / name)

  def prepareCode(path: os.Path | os.SubPath): ZIO[Scope, Throwable, os.Path] =
    path match {
      case p: os.Path    =>
        ZIO.attemptBlocking {
          require(os.isFile(p), s"Missing code archive: $path")
          p
        }
      case p: os.SubPath =>
        ZIO.attemptBlocking {
          val res = stagingDir / p
          require(os.isFile(res), s"Missing code archive: $res")
          res
        }
    }

  def deleteCode(id: ControllerTypeId): Task[Unit] =
    data.update(_ - (os.SubPath("code") / s"$id.tar.gz"))

  private def publish(path: os.Path | os.SubPath, target: os.SubPath): Task[os.SubPath] = {
    import os.{GlobSyntax, /}
    for {
      rootPath                   <- root.get
      (base, targetPath: os.Path) = path match {
                                      case p / g"images" / rest => (p, p / "images" / rest)
                                      case p / g"code" / rest   => (p, p / "code" / rest)
                                      case p: os.SubPath        => (rootPath, rootPath / p)
                                      case _                    =>
                                        throw new IllegalArgumentException(
                                          s"Unexpected path: $path"
                                        )
                                    }
      content                    <- ZIO.attemptBlocking(os.read.bytes(targetPath))
      _                          <- data.update(_ + (target -> Chunk.fromArray(content)))
      res                        <-
        ZIO
          .attempt(targetPath.subRelativeTo(base))
          .mapError(e =>
            new Exception(
              s"Failed to compute relative path for $targetPath rel to $base (args: path=$path, target=$target)",
              e
            )
          )
    } yield res
  }

  def saveImage(content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, os.SubPath] =
    for {
      chunk   <- content.runCollect
      newPath <- Random.nextUUID.map(uuid => os.SubPath("images") / s"$uuid.png")
      _       <- data.update(_ + (newPath -> chunk))
    } yield newPath

  def stageCode(
    files: ZStream[Any, Throwable, (String, ZStream[Any, Throwable, Byte])]
  ): Task[os.SubPath] =
    for {
      name <- Random.nextUUID.map(uuid => s"$uuid.tar.gz")
      _    <- files.runForeach {
                case (path, content) =>
                  content
                    .runCollect
                    .flatMap(c => data.update(_ + (os.SubPath(name) / os.SubPath(path) -> c)))
              }
    } yield os.SubPath(name)

  def publishImage(path: os.Path, name: String): zio.Task[os.SubPath] =
    publish(path, os.SubPath("images") / name)

  def touch(path: os.Path): zio.Task[Unit] = ZIO.unit

  def setRoot(root: os.Path): UIO[Unit] =
    this.root.set(root)
}
object StaticServiceFake {

  def live: ULayer[StaticServiceFake] =
    ZLayer {
      for {
        data <- Ref.make(Map.empty[os.SubPath, Chunk[Byte]])
        lock <- Semaphore.make(1)
        root <- Ref.make(os.pwd)
      } yield new StaticServiceFake(data, lock, root)
    }

  def saveImage(content: ZStream[Any, Throwable, Byte]) =
    ZIO.serviceWithZIO[StaticServiceFake](_.saveImage(content))

  def setRoot(root: os.Path) =
    ZIO.serviceWithZIO[StaticServiceFake](_.setRoot(root))
}
