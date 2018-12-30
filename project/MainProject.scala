import ArduinoPlugin.autoImport._
import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ
import JnaeratorPlugin.autoImport._
import sbt.Keys._
import sbt._

object MainProject {

  lazy val mainProject = (project in file("."))
    .enablePlugins(JnaeratorPlugin, ArduinoPlugin)
    .settings {

      version := "0.1"
      scalaVersion := "2.12.7"
      resolvers += Resolver.bintrayRepo("jarlakxen", "maven")

      name := "raspberry-farm"
      mainClass := Some("com.ilyak.pifarm.Main")

      scalacOptions ++= Seq(
        //"-Xfatal-warnings",
        "-Ypartial-unification"
      )

      Jnaerator / jnaeratorTargets += JnaeratorTarget(
        headerFile = baseDirectory.value / "lib" / "all.h",
        packageName = "com.ilyak.wiringPi",
        libraryName = "wiringPi",
        extraArgs = Seq(s"-I${(baseDirectory.value / "lib").getCanonicalPath}")
      )

      Jnaerator / jnaeratorRuntime := BridJ

      Compile / resourceGenerators += Def.task {
        import scala.sys.process._
        "npm start" !

        val path = s"${(Compile / resourceDirectory).value.getAbsolutePath}/interface"
        val dir = new File(s"$path/web")
        dir.listFiles().toSeq :+ new File(s"$path/index.html")
      }

      Arduino / arduinos := Map(
        "ttyUSB" -> "arduino:avr:nano:cpu=atmega328old",
        "ttyACM" -> "arduino:avr:uno"
      )
    }
}
