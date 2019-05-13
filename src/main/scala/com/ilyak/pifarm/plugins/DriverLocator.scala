package com.ilyak.pifarm.plugins

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.LoggingAdapter
import akka.stream.ActorMaterializer
import com.ilyak.pifarm.ManifestLocator
import com.ilyak.pifarm.Types.TDriverCompanion
import com.ilyak.pifarm.driver.Driver.Connector
import com.ilyak.pifarm.io.device.arduino.DriverManifest

class DriverLocator(manifests: Map[String, TDriverCompanion]) {
  def createInstance(name: String)
                    (implicit s: ActorSystem,
                     m: ActorMaterializer,
                     loader: ActorRef,
                     log: LoggingAdapter): Option[Connector] =
    manifests
      .get(name)
      .map(c => c.connector(loader))
}

object DriverLocator extends ManifestLocator {
  def apply(pluginDir: String): DriverLocator =
    new DriverLocator(
      locate[DriverManifest](pluginDir)
        .foldLeft(Map.empty[String, TDriverCompanion])(_ ++ _.drivers.map(d => d.name -> d))
    )
}
