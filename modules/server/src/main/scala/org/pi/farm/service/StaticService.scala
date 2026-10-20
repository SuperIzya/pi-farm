package org.pi.farm.service
import org.pi.farm.utils.ConfigCompanion

import zio.*
import zio.stream.{ZSink, ZStream}

import java.nio.file.{Files, Path}
import java.util.Base64

trait StaticService {
  def getStaticResource(path: Path): Task[(Int, ZStream[Any, Throwable, Byte])]
  def getStaticResource(path: String): Task[(Int, ZStream[Any, Throwable, Byte])] =
    getStaticResource(Path.of(path))

  def saveImage(content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String]
  def saveImage(content: Chunk[Byte]): ZIO[Any, Throwable, String] =
    saveImage(ZStream.fromChunk(content))
  def saveImage(base64: String): ZIO[Any, Throwable, String]       = ZIO
    .attempt {
      val bytes = Base64.getDecoder.decode(base64)
      Chunk.fromArray(bytes)
    }
    .flatMap(content => saveImage(ZStream.fromChunk(content)))

  def listImages: Task[Set[Path]]
  def deleteImage(path: Path): Task[Boolean]
}

object StaticService {
  case class Config(baseDir: String)
  object Config extends ConfigCompanion[Config]("static-service")

  def live: URLayer[Config, StaticService] = ZLayer.fromFunction(new Live(_))

  private final class Live(config: Config) extends StaticService {

    private val baseDir      = Path.of(config.baseDir)
    private val imagesRelDir = Path.of("images")

    def listImages: Task[Set[Path]] =
      ZIO.attempt {
        baseDir
          .resolve(imagesRelDir)
          .toFile
          .listFiles()
          .toSet
          .map { file =>
            val path = file.toPath()
            baseDir.relativize(path)
          }
      }

    def deleteImage(path: Path): Task[Boolean] =
      ZIO.attempt {
        val file = baseDir.resolve(path).toFile
        if (file.exists()) file.delete() else false
      }

    def getStaticResource(path: Path): Task[(Int, ZStream[Any, Throwable, Byte])] =
      ZIO
        .attempt {
          val fullPath = baseDir.resolve(path).toFile.getCanonicalFile.toPath
          require(
            fullPath.startsWith(baseDir.toFile.getCanonicalFile.toPath),
            s"Resource outside static directory: $path"
          )
          val file     = fullPath.toFile()

          if (file.exists()) {
            val data = ZStream.fromPath(fullPath)
            (file.length.toInt, data)
          } else {
            val data = ZStream.fromResource(path.toString)
            (0, data)
          }
        }
        .orElseFail(new Exception(s"Resource not found: $path"))

    def saveImage(content: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, String] =
      for {
        newName <- Random.nextUUID.map(uuid => imagesRelDir.resolve(s"$uuid.png"))
        newFile  = baseDir.resolve(newName).toFile
        _       <- ZIO.attempt(Files.createDirectories(newFile.toPath.getParent))
        size    <- content.run(ZSink.fromFile(newFile))
        _       <- ZIO.logInfo(s"Saved image to $newName with size $size bytes")
      } yield newName.toString()

  }
}
