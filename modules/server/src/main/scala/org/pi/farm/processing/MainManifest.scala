package org.pi.farm.processing

import org.pi.farm.plugin.{DataProcessor, Service}
import zio.*

object MainManifest extends org.pi.farm.plugin.Manifest {
  val version: String = "0.1.0"
  val name: String    = "Main Processing Modules"

  val services: Chunk[Service.Creator] =
    Chunk(
      Discovery.service
    )

  val processors: Chunk[DataProcessor] =
    Chunk.empty

}
