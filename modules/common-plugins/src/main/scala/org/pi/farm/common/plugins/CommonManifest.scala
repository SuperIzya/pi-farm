package org.pi.farm.common.plugins

import org.pi.farm.common.plugins.processors.*
import org.pi.farm.plugin.Manifest
import zio.*
import org.pi.farm.plugin.{Service, DataProcessor}

object CommonManifest extends Manifest {
  val version: String = "0.1.0"
  val name: String    = "Common Plugins"

  val processors: Chunk[DataProcessor] =
    Chunk(
      PlantWatering,
      PingPong
    )

  val services: Chunk[Service.Creator] =
    Chunk(
      Heartbeat.service
    )

}
