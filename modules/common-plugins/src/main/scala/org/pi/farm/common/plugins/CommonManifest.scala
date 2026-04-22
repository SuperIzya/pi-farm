package org.pi.farm.common.plugins

import org.pi.farm.common.plugins.processors.*
import org.pi.farm.plugin.{DataProcessor, Manifest, Service}

import zio.*

object CommonManifest extends Manifest {
  val version: String = "0.1.0"
  val name: String    = "Common Plugins"

  val processors: Chunk[DataProcessor] =
    Chunk(
      PlantWatering
    )

  val services: Chunk[Service.Creator] =
    Chunk(
      Heartbeat.service,
      PingPong.service
    )

}
