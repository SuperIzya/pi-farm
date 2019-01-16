package com.ilyak.pifarm.sdk

import com.ilyak.pifarm.sdk.configuration.GeneralBlock
import com.ilyak.pifarm.sdk.meta.BlockDescription

/***
  * Manifest, that each good plugin should have.
  */
trait PiManifest {
  /***
    * General name of the plugin
    */
  val pluginName: String

  /***
    * All public blocks introduced by this plugin
    */
  val blocks: Map[BlockDescription, Any => GeneralBlock]

}
