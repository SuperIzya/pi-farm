package org.pi.farm.fake

import org.pi.farm.service.StaticService

import zio.*
import zio.stream.*

final class StaticServiceFake(data: Ref[Map[String, Chunk[Byte]]]) extends StaticService {

  def reset: UIO[Unit] = data.set(Map.empty)

  def getStaticResource(path: String): UIO[(Int, ZStream[Any, Throwable, Byte])] =
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
      _     <- data.update(_ + (path -> chunk))
    } yield path

}
object StaticServiceFake {

  def live: ULayer[StaticServiceFake] =
    ZLayer {
      for {
        data <- Ref.make(Map.empty[String, Chunk[Byte]])
      } yield new StaticServiceFake(data)
    }
}
