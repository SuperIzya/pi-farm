package com.ilyak.pifarm

import java.io.File

import akka.event.LoggingAdapter

import io.github.classgraph.ClassGraph
import scala.reflect.ClassTag

trait ManifestLocator {
  private def allJarFiles(f: File): Boolean = {
    val name = f.getName
    val index = name.lastIndexOf(".")
    name.substring(index + 1) == "jar"
  }

  def locate[T: ClassTag](log: LoggingAdapter): Seq[T] = {
    import scala.collection.JavaConverters._

    val res = new ClassGraph()
      .enableAllInfo()
      .scan()

    val classes = res.getSubclasses(implicitly[ClassTag[T]].runtimeClass.getName)
      .loadClasses()
      .asScala
      .toList

    try {
      classes
        .flatMap(cls => {
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
        log.error(s"Error while loading classes: $err")
        Seq.empty
    }
  }
}
