package org.pi.farm.service
import org.pi.farm.utils.ConfigCompanion

import zio.{Random, Scope, ULayer, URLayer, ZIO, ZLayer}
import zio.stream.{ZSink, ZStream}

trait StaticService {
  def getStaticResource(path: String): ZStream[Any, Throwable, Byte]
  def saveImage(path: String, content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String]
}

object StaticService {
  case class Config(baseDir: String)
  object Config extends ConfigCompanion[Config]("static-service")

  def live: URLayer[Config, StaticService] = ZLayer.fromFunction(new Live(_))

  private final class Live(config: Config) extends StaticService {
    def getStaticResource(path: String): ZStream[Any, Throwable, Byte] =
      ZStream
        .fromFileName(config.baseDir + path)
        .orElse(ZStream.fromResource(path))

    def saveImage(path: String, content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String] = {
      val file = new java.io.File(config.baseDir + path)
      for {
        exists  <- ZIO.attempt {
                     file.exists()
                   }
        newPath <- if (exists) Random.nextUUID.map(uuid => s"/images/$uuid.png") else ZIO.succeed(path)
        _       <- ZIO.attempt(file.getParentFile.mkdirs()).unless(exists)

        _ <- content.run(ZSink.fromFileName(config.baseDir + newPath))
      } yield newPath
    }

  }
}
