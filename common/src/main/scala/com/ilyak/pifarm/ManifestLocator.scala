package com.ilyak.pifarm

import java.io.File

import org.clapper.classutil.ClassFinder

import scala.reflect.ClassTag

trait ManifestLocator {
  private def allJarFiles(f: File): Boolean = {
    val name = f.getName
    val index = name.lastIndexOf(".")
    name.substring(index + 1) == "jar"
  }
  def locate[T: ClassTag](dir: String): Iterator[T] = {
    val file = new File(dir)
    val pluginJars: Seq[File] = {
      if (file.isDirectory) file.listFiles().filter(allJarFiles)
      else if (file.getAbsolutePath.endsWith(".jar")) Seq(file)
      else Seq.empty
    }
    val finder = ClassFinder(pluginJars)
    try {
      ClassFinder
        .concreteSubclasses(implicitly[ClassTag[T]].runtimeClass, finder.getClasses())
        .map(info => Class.forName(info.name).newInstance().asInstanceOf[T])
    }
    catch {
      case _: Throwable => List.empty[T].iterator
    }
  }
}
