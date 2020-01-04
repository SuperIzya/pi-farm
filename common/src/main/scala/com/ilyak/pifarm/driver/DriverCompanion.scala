package com.ilyak.pifarm.driver

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.jar.JarFile

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.driver.DriverCompanion.TDriverCompanion
import com.ilyak.pifarm.driver.LoaderActor.CancelLoad
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.types.{Result, WrapFlow}

import scala.concurrent.{Await, TimeoutException}
import scala.language.{implicitConversions, postfixOps}
import scala.util.Random

trait DriverCompanion[TDriver <: Driver] extends TDriverCompanion {

  case class Sources(source: String, dependencies: Seq[String])
  object Sources {
    def apply(sources: Seq[String]): Sources =
      new Sources(sources.head, sources.tail)
    def apply(source: String): Sources = new Sources(source, Seq.empty)

    implicit def toSources(sources: Iterable[String]): Sources =
      Sources(sources.toSeq)
    implicit def toSources(source: String): Sources = Sources(source)
  }

  val source: Sources
  val driver: TDriver
  val loader: ClassLoader = getClass.getClassLoader

  private var sourceFile: File = _

  def resourcePath(res: String): String = loader.getResource(res).getPath

  def command(device: String, source: String): Result[String]

  def connector(
    loader: ActorRef,
    deviceProps: Props
  )(implicit s: ActorSystem, a: ActorMaterializer): Connector =
    Connector(
      name,
      (deviceId, connector) =>
        loadController(deviceId, loader)
          .flatMap(
            _ =>
              driver
                .connector(deviceProps)
                .wrapFlow(connector.wrap)
                .connect(deviceId)
        )
    )

  def wrap(wrap: WrapFlow, deviceProps: Props, loader: ActorRef)(
    implicit s: ActorSystem,
    mat: ActorMaterializer
  ): Connector =
    connector(loader, deviceProps).wrapFlow(wrap)

  protected def getControllersCode: File = {
    if (sourceFile == null) {
      val dir = Files.createTempDirectory(Random.nextLong.toString)
      val arr = resourcePath(source.source).split("!/")
      val jar = new JarFile(arr(0).replace("file:", ""))
      def processResource(r: String): File = {
        val entry = jar.getEntry(r)
        val fileName = Paths.get(r).toFile.getName
        val f = Files.createFile(Paths.get(dir.toString, fileName)).toFile
        f.deleteOnExit()
        Files.copy(
          jar.getInputStream(entry),
          f.toPath,
          StandardCopyOption.REPLACE_EXISTING
        )
        f
      }
      sourceFile = processResource(arr(1))

      source.dependencies
        .map(resourcePath)
        .map(_.split("!/")(1))
        .foreach(processResource)
    }
    sourceFile
  }

  def loadController(deviceId: String, loader: ActorRef)(
    implicit s: ActorSystem
  ): Result[Unit] = {
    command(deviceId, getControllersCode.getAbsolutePath)
      .map(cmd => {
        import s.dispatcher

        import scala.concurrent.duration._

        val duration = 1 minute
        implicit val timeout: Timeout = duration
        val future = (loader ? LoaderActor.Load(deviceId, cmd))
          .map(_.asInstanceOf[Boolean])
        try {
          Await.result[Boolean](future, duration)
        } catch {
          case _: TimeoutException =>
            loader ! CancelLoad
            false
        }
      })
      .flatMap { res =>
        if (res) Result.Res(Unit)
        else
          Result.Err(
            s"Some error occurred while loading $name to $deviceId ($res)"
          )
      }
  }
}

object DriverCompanion {

  trait TDriverCompanion {
    val name: String
    val meta: Map[String, String]
    val defaultConfigurations: List[Configuration.Graph]

    def connector(loader: ActorRef, deviceProps: Props)(
      implicit s: ActorSystem,
      mat: ActorMaterializer
    ): Connector

    def wrap(wrap: WrapFlow, deviceProps: Props, loader: ActorRef)(
      implicit s: ActorSystem,
      mat: ActorMaterializer
    ): Connector
  }

}
