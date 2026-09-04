package org.pi.farm.service
import org.pi.farm.utils.ConfigCompanion

import zio.{Random, Scope, Task, ULayer, URLayer, ZIO, ZLayer}
import zio.stream.{ZSink, ZStream}

trait StaticService {
  def getStaticResource(path: String): Task[(Int, ZStream[Any, Throwable, Byte])]
  def saveImage(path: String, content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String]
}

object StaticService {
  case class Config(baseDir: String)
  object Config extends ConfigCompanion[Config]("static-service")

  def live: URLayer[Config, StaticService] = ZLayer.fromFunction(new Live(_))

  private final class Live(config: Config) extends StaticService {
    def getStaticResource(path: String): Task[(Int, ZStream[Any, Throwable, Byte])] =
      ZIO
        .attempt {
          val file = new java.io.File(config.baseDir + path)
          if (file.exists()) {
            val data = ZStream.fromFile(file)
            (file.length.toInt, data)
          } else {
            val data = ZStream.fromResource(path)
            (0, data)
          }
        }
        .orElseSucceed((0, ZStream.empty))

    def saveImage(path: String, content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String] = {
      def normalize(path: String): String =
        if (path.startsWith("images/")) path else s"images/$path"

      val normalizedPath = normalize(path)
      val file           = new java.io.File(config.baseDir + normalizedPath)
      for {
        exists  <- ZIO.attempt {
                     file.exists()
                   }
        newName <- Random.nextUUID.map(uuid => normalize(s"$uuid.png")).when(exists).someOrElse(normalizedPath)

        newFile = new java.io.File(config.baseDir + newName)

        _ <- ZIO.attempt(newFile.getParentFile.mkdirs()).unless(exists)
        _ <- content.run(ZSink.fromFile(newFile))
      } yield newName
    }

  }
}
