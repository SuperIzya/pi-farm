package com.ilyak.pifarm.plugins

import java.io.File

import com.ilyak.pifarm.sdk.PiManifest
import org.clapper.classutil.ClassFinder

class PluginLocator private(manifests: Map[String, PiManifest]) {

}

object PluginLocator {
  private   def allJarFiles(f: File): Boolean = {
    val name = f.getName
    val index = name.lastIndexOf(".")
    name.substring(index + 1) == "jar"
  }

  def apply(pluginDir: String) = {

    val pluginJars = new File(pluginDir).listFiles().filter(allJarFiles)
    val finder = ClassFinder(pluginJars)
    val pluginClasses = ClassFinder.concreteSubclasses(classOf[PiManifest], finder.getClasses())
    val plugins = pluginClasses.map(info => info -> Class.forName(info.name).newInstance().asInstanceOf[PiManifest]).toMap

  }
}
