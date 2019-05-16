package com.ilyak.pifarm

import java.io.File

import akka.event.LoggingAdapter
import org.clapper.classutil.{ ClassFinder, ClassInfo }

import scala.reflect.ClassTag

trait ManifestLocator {
  private def allJarFiles(f: File): Boolean = {
    val name = f.getName
    val index = name.lastIndexOf(".")
    name.substring(index + 1) == "jar"
  }

  def locate[T: ClassTag](dir: String, log: LoggingAdapter): Seq[T] = {
    val file = new File(dir)
    val pluginJars: Seq[File] = {
      if (file.isDirectory) file.listFiles().filter(allJarFiles)
      else if (file.getAbsolutePath.endsWith(".jar")) Seq(file)
      else Seq.empty
    }
    try {
      val classes: Seq[ClassInfo] = ClassFinder(pluginJars).getClasses()
      val className = implicitly[ClassTag[T]].runtimeClass.getName
      classes
        .filter(_.superClassName == className)
        .flatMap(info => {
          val cls = Class.forName(info.name)
            try {
              Seq(cls.newInstance().asInstanceOf[T])
            }
          catch {
            case _: IllegalAccessException => try {
              Seq(cls.getField("MODULE$").get(cls).asInstanceOf[T])
            } catch {
              case _: Throwable => Seq.empty
            }
            case _: Throwable => Seq.empty
          }
        })
    }
    catch {
      case err: Throwable =>
        log.error(s"Error while loading $dir: $err")
        Seq.empty
    }
  }
}
