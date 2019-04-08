package com.ilyak.pifarm.plugins.basic

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.flow.configuration.BlockDescription.TBlockDescription

object Manifest extends PiManifest {
  /** *
    * General name of the plugin
    */
  override val pluginName: String = "Basic plugins"
  /** *
    * All public blocks introduced by this plugin
    */
  override val blockDescriptions: Seq[TBlockDescription] = ???
    //Seq(BlockDescription("schedule", Container))
}
