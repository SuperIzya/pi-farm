package com.ilyak.pifarm.plugins.basic

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.flow.configuration.BlockDescription
import com.ilyak.pifarm.flow.configuration.BlockType.Container

object Manifest extends PiManifest {
  /** *
    * General name of the plugin
    */
  override val pluginName: String = "Basic plugins"
  /** *
    * All public blocks introduced by this plugin
    */
  override val allBlocks: Seq[BlockDescription] = Seq(
    BlockDescription("schedule", Container)
  )
}
