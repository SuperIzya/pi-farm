package com.ilyak.pifarm.flow.actors

import java.io.{ File, FileFilter }
import java.nio.file.{ FileSystems, Paths, StandardWatchEventKinds }

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.Devices
import com.typesafe.config.Config

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.language.postfixOps

class DeviceScanActor(driverRegistry: ActorRef, patternStrings: List[String])
  extends Actor
    with ActorLogging {
  log.debug("Starting...")
  import context.dispatcher

  val patterns = patternStrings.map(_.r.unanchored)

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
  var devices = scan
  log.debug(s"Initial scan complete (${devices.size} devices)")
  @tailrec
  private def watch(): Unit = {
    val key = watcher.take
    val events = key.pollEvents

    if (!events.isEmpty) self ! 'change
    if (key.reset()) watch()
  }

  Future { try { watch() } finally {} }

  def scan = root.toFile.listFiles(new FileFilter {
    override def accept(file: File): Boolean = patterns.exists(_.findFirstMatchIn(file.getName).isDefined)
  }).map(_.getAbsolutePath).toSet

  override def receive: Receive = {
    case 'change =>
      val d = scan
      if (d != devices) {
        devices = d
        driverRegistry ! Devices(devices)
      }
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
