package com.ilyak.pifarm.driver

import java.io.File
import java.nio.file.{ Files, Paths, StandardCopyOption }
import java.util.jar.JarFile

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.ilyak.pifarm.Types.{ Result, WrapFlow }
import com.ilyak.pifarm.driver.Driver.Connections
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.{ Decoder, Encoder, Result }

import scala.concurrent.Await
import scala.language.postfixOps

//@formatter:off
abstract class DriverCompanion[C : Encoder,
                               D : Decoder,
                               TDriver <: Driver[C, D]] extends TDriverCompanion {
//@formatter:on


  val source: String
  val driver: TDriver
  private var sourceFile: File = _

  def command(device: String, source: String): Result[String]

  def apply(deviceId: String)
           (implicit s: ActorSystem,
            mat: ActorMaterializer): Result[Connections] =
  loadController(deviceId).flatMap { _ =>
      driver.connect[C, D](deviceId)
    }


  def wrap(wrap: WrapFlow)
          (implicit s: ActorSystem,
           mat: ActorMaterializer): String => Result[Connections] = deviceId =>
    loadController(deviceId).flatMap(_ => {
      val f = driver.wrapConnect[C, D](wrap)
      f(deviceId)
    })

  protected def getControllersCode: File = {
    if (sourceFile == null) {
      val arr = source.split("!/")
      val jar = new JarFile(arr(0).replace("file:", ""))
      val entry = jar.getEntry(arr(1))
      val fileName = Paths.get(arr(1)).toFile.getName
      val index = fileName.lastIndexOf(".")
      val dir = Files.createTempDirectory(fileName.substring(0, index))
      sourceFile = File.createTempFile(fileName.substring(0, index), fileName.substring(index), dir.toFile)
      sourceFile.deleteOnExit()

      Files.copy(jar.getInputStream(entry), sourceFile.toPath, StandardCopyOption.REPLACE_EXISTING)
    }
    sourceFile
  }

  def loadController(deviceId: String)
                    (implicit s: ActorSystem): Result[Unit] = {
    command(deviceId, getControllersCode.getAbsolutePath)
      .map(cmd => {
        import s.dispatcher
        import scala.concurrent.duration._

        val duration = 1 minute
        implicit val timeout: Timeout = duration
        val actor = s.actorOf(LoaderActor.props())
        val future = (actor ? cmd).map(_.asInstanceOf[Boolean])
        val res = try { Await.result[Boolean](future, duration) } finally { s.stop(actor) }
        res
      })
      .flatMap { res =>
        if (res) Result.Res(Unit)
        else Result.Err(s"Some error occurred while loading $name to $deviceId ($res)")
      }
  }
}

object DriverCompanion {

  trait TDriverCompanion {
    val name: String
    val meta: Map[String, String]

    def wrap(wrap: WrapFlow)
            (implicit s: ActorSystem,
             mat: ActorMaterializer): String => Result[Connections]

    def apply(deviceId: String)
             (implicit s: ActorSystem,
              mat: ActorMaterializer): Result[Connections]
  }



}