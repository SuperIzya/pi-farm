package com.ilyak.pifarm.plugins

import java.io.File

import com.ilyak.pifarm.PiManifest
import com.ilyak.pifarm.flow.configuration.ConfigurableShape
import com.ilyak.pifarm.flow.configuration.Configuration.MetaData
import org.clapper.classutil.ClassFinder

class PluginLocator private(manifests: Map[String, PiManifest]) {
  def createInstance(meta: MetaData): Option[ConfigurableShape] = manifests
    .get(meta.plugin)
    .flatMap(_.blocks.get(meta.blockName))
    .map(_.creator(meta))
}

/** *
  * Locates all the plugins (in separate jars only).
  * From these plugins it reads metadata which allows
  * later to locate and instantiate blocks for control flow.
  */
object PluginLocator {
  private def allJarFiles(f: File): Boolean = {
    val name = f.getName
    val index = name.lastIndexOf(".")
    name.substring(index + 1) == "jar"
  }

  def apply(pluginDir: String) = {
    val pluginJars = new File(pluginDir).listFiles().filter(allJarFiles)
    val finder = ClassFinder(pluginJars)
    val manifests = ClassFinder
      .concreteSubclasses(classOf[PiManifest], finder.getClasses())
      .map(info => Class.forName(info.name).newInstance().asInstanceOf[PiManifest])
      .map(m => m.pluginName -> m)
      .toMap

    new PluginLocator(manifests)
  }
}
