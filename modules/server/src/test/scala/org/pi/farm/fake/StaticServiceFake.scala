package org.pi.farm.fake

import org.pi.farm.model.Types.ControllerTypeId
import org.pi.farm.service.StaticService

import zio.*
import zio.stream.*

import java.nio.file.{Files, Path}

final class StaticServiceFake(data: Ref[Map[Path, Chunk[Byte]]], lock: Semaphore)
    extends StaticService {

  def withPublicationLock[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
    lock.withPermit(effect)

  def reset: UIO[Unit] = data.set(Map.empty)

  def getStaticResource(path: Path): Task[(Int, ZStream[Any, Throwable, Byte])] =
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
        _.get(Path.of("code", s"$id.tar.gz"))
          .map(content => (content.length, ZStream.fromChunk(content)))
      )

  def addResource(path: Path, content: Chunk[Byte]): UIO[Unit] =
    data.update(_ + (path -> content))

  def publishImage(path: Path, name: String): Task[Path] =
    publish(path, Path.of("images", name))

  def publishCode(path: Path, name: String): Task[Path] =
    publish(path, Path.of("code", name))

  def prepareCode(path: Path): ZIO[Scope, Throwable, Path] =
    ZIO.attemptBlocking {
      require(Files.isRegularFile(path), s"Missing code archive: $path")
      path
    }

  def deleteCode(id: ControllerTypeId): Task[Unit] =
    data.update(_ - Path.of("code", s"$id.tar.gz"))

  private def publish(path: Path, target: Path): Task[Path] =
    for {
      content <- ZIO.attemptBlocking(Files.readAllBytes(path)).map(Chunk.fromArray)
      _       <- data.update(_ + (target -> content))
    } yield target

  def saveImage(content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String] =
    for {
      chunk   <- content.runCollect
      newPath <- Random.nextUUID.map(uuid => s"images/$uuid.png")
      _       <- data.update(_ + (Path.of(newPath) -> chunk))
    } yield newPath

  def stageCode(
    files: ZStream[Any, Throwable, (String, ZStream[Any, Throwable, Byte])]
  ): Task[String] =
    for {
      name <- Random.nextUUID.map(uuid => s"$uuid.tar.gz")
      _    <- files.runForeach {
                case (path, content) =>
                  content.runCollect.flatMap(c => data.update(_ + (Path.of(name, path) -> c)))
              }
    } yield name

}
object StaticServiceFake {

  def live: ULayer[StaticServiceFake] =
    ZLayer {
      for {
        data <- Ref.make(Map.empty[Path, Chunk[Byte]])
        lock <- Semaphore.make(1)
      } yield new StaticServiceFake(data, lock)
    }

  def saveImage(base64: String) = ZIO.serviceWithZIO[StaticService](_.saveImage(base64))
}
