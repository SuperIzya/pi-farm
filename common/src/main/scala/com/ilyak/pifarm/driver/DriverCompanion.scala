package com.ilyak.pifarm.driver

import java.io.File
import java.nio.file.{ Files, Paths, StandardCopyOption }
import java.util.jar.JarFile

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.{ Result, WrapFlow }
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.driver.LoaderActor.CancelLoad
import com.ilyak.pifarm.flow.configuration.Configuration

import scala.concurrent.{ Await, TimeoutException }
import scala.language.postfixOps

trait DriverCompanion[TDriver <: Driver] extends TDriverCompanion {

  val source: String
  val driver: TDriver

  private var sourceFile: File = _

  def command(device: String, source: String): Result[String]

  def connector(loader: ActorRef, deviceProps: Props)
               (implicit s: ActorSystem, a: ActorMaterializer): Connector =
    Connector(
      name,
      (deviceId, connector) =>
        loadController(deviceId, loader)
          .flatMap(_ =>
            driver
              .connector(deviceProps)
              .wrapFlow(connector.wrap)
              .connect(deviceId)
          )
    )

  def wrap(wrap: WrapFlow,
           deviceProps: Props,
           loader: ActorRef)
          (implicit s: ActorSystem,
           mat: ActorMaterializer): Connector =
    connector(loader, deviceProps).wrapFlow(wrap)

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

  def loadController(deviceId: String,
                     loader: ActorRef)
                    (implicit s: ActorSystem): Result[Unit] = {
    command(deviceId, getControllersCode.getAbsolutePath)
      .map(cmd => {
        import s.dispatcher

        import scala.concurrent.duration._

        val duration = 1 minute
        implicit val timeout: Timeout = duration
        val future = (loader ? LoaderActor.Load(deviceId, cmd)).map(_.asInstanceOf[Boolean])
        try {
          Await.result[Boolean](future, duration)
        }
        catch {
          case _: TimeoutException =>
            loader ! CancelLoad
            false
        }
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
    val defaultConfigurations: List[Configuration.Graph]

    def connector(loader: ActorRef, deviceProps: Props)
                 (implicit s: ActorSystem,
                  mat: ActorMaterializer): Connector


    def wrap(wrap: WrapFlow,
             deviceProps: Props,
             loader: ActorRef)
            (implicit s: ActorSystem,
             mat: ActorMaterializer): Connector
  }

}
