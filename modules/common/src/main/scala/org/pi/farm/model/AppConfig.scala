package org.pi.farm.model

import org.pi.farm.model.Types.Units

import zio.Chunk
import zio.json.{DeriveJsonCodec, JsonCodec}

/** Settings shared with the UI at application startup.
  *
  * @param units
  *   measurement units available to the system
  * @param codeFileGlobs
  *   chunk of archive-relative glob patterns for files included from controller code folders,
  *   including the required project configuration `platformio.ini`
  */
final case class AppConfig(units: Set[Units], codeFileGlobs: Chunk[String])

object AppConfig {
  given JsonCodec[AppConfig] = DeriveJsonCodec.gen[AppConfig]
}
