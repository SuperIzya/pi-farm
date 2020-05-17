package com.ilyak.pifarm.flow.actors

import java.io.{File, FileFilter}
import java.nio.file.{FileSystems, Paths, StandardWatchEventKinds}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.Devices
import com.typesafe.config.Config
import zio.internal.Platform
import zio.{Ref, Task, UIO, ZIO}

import scala.annotation.tailrec
import scala.language.postfixOps

class DeviceScanActor(driverRegistry: ActorRef, patternStrings: List[String])
  extends Actor
    with ActorLogging {
  log.debug("Starting...")

  val patterns = patternStrings.map(_.r.unanchored)
  val zioRuntime = zio.Runtime(context.dispatcher, Platform.default)

  val root = Paths.get("/dev")
  log.debug("Defining watcher")
  val watcher = FileSystems.getDefault.newWatchService()
  log.debug("Watched defined")
  log.debug(s"Registering watch on $root")
  root.register(
    watcher,
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_DELETE
  )
  log.debug(s"Watch on $root registered")
  log.debug("Starting initial scan...")
  val devicesRef: Ref[Set[String]] = runZIO {
    for {
      list <- scan
      res <- Ref.make(list)
      _ <- Task.effectTotal(log.debug(s"Initial scan complete (${list.size} devices)"))
      _ <- Task.effectTotal {
        driverRegistry ! Devices(list)
      }
    } yield res
  }

  val change: Task[Unit] = for {
    devices <- devicesRef.get
    d <- scan
    _ <- if (d != devices) {
      Task.effectTotal(driverRegistry ! Devices(d)) *>
        devicesRef.set(d)
    }
    else Task.succeed(())
  } yield ()

  override def receive: Receive = {
    case 'start => watch()
    case 'change => runZIO (change)
  }

  private def watch(): Unit = {
    val changeTask: Task[Unit] = UIO.succeed(log.debug("Changing devices")) *> change
    lazy val zioWatch: Task[Unit] = for {
      key <- Task.effectTotal(watcher.take)
      _ <- if(key.pollEvents().isEmpty) Task.succeed(()) else changeTask
      _ <- if(key.reset()) zioWatch else Task.succeed(())
    } yield ()

    runZIO {
      zioWatch.catchAll(_ => ZIO.succeed(())).fork
    }
  }

  def runZIO[U](zio: Task[U]): U = zioRuntime.unsafeRun(zio)

  def scan: Task[Set[String]] = Task.effectTotal {
    root.toFile.listFiles(new FileFilter {
      override def accept(file: File): Boolean = patterns.exists(_.findFirstMatchIn(file.getName).isDefined)
    }).map(_.getAbsolutePath).toSet
  }

  override def postStop(): Unit = {
    watcher.close()
    super.postStop()
  }

  log.debug("Started")
}

object DeviceScanActor {

  import scala.collection.JavaConverters._

  def props(registry: ActorRef, config: Config): Props = {
    val patterns = config.getStringList("patterns").asScala
    Props(new DeviceScanActor(registry, patterns.toList))
  }
}
