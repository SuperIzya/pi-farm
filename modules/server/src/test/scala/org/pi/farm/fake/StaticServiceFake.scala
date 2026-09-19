package org.pi.farm.fake

import org.pi.farm.service.StaticService

import zio.*
import zio.stream.*

import java.nio.file.Path

final class StaticServiceFake(data: Ref[Map[Path, Chunk[Byte]]]) extends StaticService {

  override def listImages: Task[Set[Path]] = data.get.map(_.keySet)

  override def deleteImage(path: Path): Task[Boolean] =
    data.modify { map =>
      if (map.contains(path)) (true, map - path)
      else (false, map)
    }

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

  def saveImage(path: String, content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String] =
    for {
      chunk <- content.runCollect
      _     <- data.update(_ + (Path.of(path) -> chunk))
    } yield path

}
object StaticServiceFake {

  def live: ULayer[StaticServiceFake] =
    ZLayer {
      for {
        data <- Ref.make(Map.empty[Path, Chunk[Byte]])
      } yield new StaticServiceFake(data)
    }
}
