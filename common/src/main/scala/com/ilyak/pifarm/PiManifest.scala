package com.ilyak.pifarm

import com.ilyak.pifarm.flow.configuration.BlockDescription.TBlockDescription

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
  val blockDescriptions: Seq[TBlockDescription]
  /***
    * Map of all blocks by name introduced by this plugins
    */
  lazy val descriptionsMap: Map[String, TBlockDescription] =
    blockDescriptions.map(b => b.name -> b).toMap
}

object PiManifest {
  implicit val ev: Class[PiManifest] = classOf[PiManifest]
}