package com.ilyak.pifarm

import java.io.File

import org.clapper.classutil.ClassFinder

trait ManifestLocator {
  private def allJarFiles(f: File): Boolean = {
    val name = f.getName
    val index = name.lastIndexOf(".")
    name.substring(index + 1) == "jar"
  }
  def locate[T](dir: String): Iterator[T] = {
    val pluginJars = new File(dir).listFiles().filter(allJarFiles)
    val finder = ClassFinder(pluginJars)
    ClassFinder
      .concreteSubclasses(classOf[T], finder.getClasses())
      .map(info => Class.forName(info.name).newInstance().asInstanceOf[T])
  }
}
