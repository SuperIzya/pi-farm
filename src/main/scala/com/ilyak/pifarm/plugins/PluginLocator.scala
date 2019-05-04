package com.ilyak.pifarm.plugins

import com.ilyak.pifarm.flow.configuration.ConfigurableNode
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo }
import com.ilyak.pifarm.{ ManifestLocator, PiManifest, RunInfo, SystemImplicits }

case class PluginLocator(system: SystemImplicits,
                         runInfo: RunInfo,
                         manifests: Map[String, PiManifest])

/** *
  * Locates all the plugins (in separate jars only).
  * From these plugins it reads metadata which allows
  * later to locate and instantiate blocks for control flow.
  */
object PluginLocator extends ManifestLocator {
  def apply(pluginDir: String, system: SystemImplicits): PluginLocator =
    new PluginLocator(
      system,
      RunInfo.empty,
      locate[PiManifest](pluginDir)
        .map(m => m.pluginName -> m)
        .toMap
    )

  implicit class Ops(val locator: PluginLocator) extends AnyVal {
    def createInstance(meta: MetaData): Option[ConfigurableNode[_]] =
      locator.manifests
        .get(meta.plugin)
        .flatMap(_.descriptionsMap.get(meta.blockName))
        .map(_.creator(parserInfo(meta)))

    def parserInfo(metaData: MetaData): MetaParserInfo =
      MetaParserInfo(metaData, locator.system, locator.runInfo)

    def forRun(info: RunInfo): PluginLocator = locator.copy(runInfo = info)
  }

}
