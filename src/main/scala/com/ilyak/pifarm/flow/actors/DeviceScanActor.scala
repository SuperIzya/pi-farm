package com.ilyak.pifarm.flow.actors

import java.io.{ File, FileFilter }
import java.nio.file.{ FileSystems, Paths, StandardWatchEventKinds }

import akka.actor.{ Actor, ActorRef, PoisonPill, Props }
import com.ilyak.pifarm.flow.actors.DriverRegistryActor.Devices
import com.typesafe.config.Config

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.language.postfixOps

class DeviceScanActor(driverRegistry: ActorRef, patternStrings: List[String]) extends Actor {

  import context.dispatcher

  val patterns = patternStrings.map(_.r.unanchored)

  val root = Paths.get("/dev")
  val watcher = FileSystems.getDefault.newWatchService()
  root.register(
    watcher,
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_DELETE
  )

  var devices = scan

  @tailrec
  private def watch(): Unit = {
    val key = watcher.take
    val events = key.pollEvents

    if(!events.isEmpty) self ! 'change
    if(key.reset()) watch()
  }
  Future { try { watch() } }

  def scan = root.toFile.listFiles(new FileFilter {
    override def accept(file: File): Boolean = patterns.exists(_.findFirstMatchIn(file.getName).isDefined)
  }).map(_.getAbsolutePath).toSet

  override def receive: Receive = {
    case 'change =>
      val d = scan
      if(d != devices) {
        devices = d
        driverRegistry ! Devices(devices)
      }
    case PoisonPill => watcher.close()
  }
}

object DeviceScanActor {
  def props(registry: ActorRef, config: Config): Props =
    Props(new DeviceScanActor(registry, config.getStringList("patterns").toList))
}
