package com.ilyak.pifarm.plugins

import com.ilyak.pifarm.flow.configuration.ConfigurableNode
import com.ilyak.pifarm.flow.configuration.Configuration.MetaData
import com.ilyak.pifarm.{ ManifestLocator, PiManifest }

class PluginLocator(manifests: Map[String, PiManifest]) {
  def createInstance(meta: MetaData): Option[ConfigurableNode[_]] = manifests
    .get(meta.plugin)
    .flatMap(_.descriptionsMap.get(meta.blockName))
    .map(_.creator(meta))
}

/** *
  * Locates all the plugins (in separate jars only).
  * From these plugins it reads metadata which allows
  * later to locate and instantiate blocks for control flow.
  */
object PluginLocator extends ManifestLocator {
  def apply(pluginDir: String): PluginLocator =
    new PluginLocator(locate[PiManifest](pluginDir)
      .map(m => m.pluginName -> m)
      .toMap
    )
}
