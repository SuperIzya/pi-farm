package com.ilyak.pifarm

import com.ilyak.pifarm.flow.configuration.BlockDescription

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
  val allBlocks: Seq[BlockDescription]
  /***
    * Map of all blocks by name introduced by this plugins
    */
  val blocks: Map[String, BlockDescription] = allBlocks.map(b => b.name -> b).toMap
}
