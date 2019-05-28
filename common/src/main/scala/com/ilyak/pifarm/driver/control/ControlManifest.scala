package com.ilyak.pifarm.driver.control

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.flow.configuration.BlockDescription.TBlockDescription
import com.ilyak.pifarm.flow.configuration.{ BlockDescription, BlockType }

object ControlManifest extends PiManifest {
  /** *
    * General name of the plugin
    */
  override val pluginName: String = "default-control"
  /** *
    * All public blocks introduced by this plugin
    */
  override val blockDescriptions: Seq[TBlockDescription] = Seq(
    BlockDescription(ControlFlow.name, ControlFlow(_), BlockType.Automaton)
  )
}
