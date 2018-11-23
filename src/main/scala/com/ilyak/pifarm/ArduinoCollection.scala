package com.ilyak.pifarm

import java.io.{File, FilenameFilter}


object ArduinoCollection {
  def apply(): Map[String, Arduino] = new File("/dev")
      .listFiles(new FilenameFilter {
        override def accept(file: File, s: String): Boolean = s.startsWith("ttyACM")
      })
      .toList
      .map(f => f.getName -> f.getAbsolutePath)
      .map(p => p._1 -> Arduino(p._2))
      .toMap
}


