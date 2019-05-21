package com.ilyak.pifarm.driver

import java.io.File
import java.nio.file.{ Files, Paths, StandardCopyOption }
import java.util.jar.JarFile

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.ilyak.pifarm.Types.{ Result, WrapFlow }
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.driver.LoaderActor.CancelLoad
import com.ilyak.pifarm.{ Decoder, Encoder, Result }

import scala.concurrent.{ Await, TimeoutException }
import scala.language.postfixOps
import scala.reflect.ClassTag

trait DriverCompanion[C, D, TDriver <: Driver[C, D]] extends TDriverCompanion {

  val source: String
  val driver: TDriver

  val encoder: Encoder[C]
  val decoder: Decoder[D]

  private var sourceFile: File = _

  def encode[Cmd <: C : Encoder : ClassTag]: PartialFunction[C, String] = {
    case x: Cmd => Encoder[Cmd].encode(x)
  }
  def decode[Data <: D : Decoder : ClassTag]: PartialFunction[String, Iterable[D]] = {
    case x if Decoder[Data].test(x) => Decoder[Data].decode(x)
  }

  def command(device: String, source: String): Result[String]

  def connector(loader: ActorRef, deviceProps: Props)
               (implicit s: ActorSystem, a: ActorMaterializer): Connector =
    Connector(
      name,
      (deviceId, connector) =>
        loadController(deviceId, loader)
          .flatMap(_ =>
            driver
              .connector(deviceProps, encoder, decoder)
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
        val future = (loader ? cmd).map(_.asInstanceOf[Boolean])
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
