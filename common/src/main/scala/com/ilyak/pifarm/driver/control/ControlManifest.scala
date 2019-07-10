package com.ilyak.pifarm.driver.control

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.flow.configuration.BlockDescription
import com.ilyak.pifarm.flow.configuration.BlockDescription.TBlockDescription

object ControlManifest extends PiManifest {
  /** *
    * General name of the plugin
    */
  override val pluginName: String = "default-control"
  /** *
    * All public blocks introduced by this plugin
    */
  override val blockDescriptions: Seq[TBlockDescription] = Seq(
    BlockDescription[ControlFlow]
  )
}
