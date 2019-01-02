import Dependencies._
import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ

import scala.language.postfixOps

ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.8"

val runAll = inputKey[Unit]("run all together")

runAll := Def.inputTaskDyn {
  import sbt.complete.Parsers.spaceDelimited
  val args = spaceDelimited("<args>").parsed
    .foldLeft(" ") {
      _ + " " + _
    }
  Def.taskDyn {
    (Arduino / upload).value
    (Compile / (migrations / run)).toTask("").value
    (Compile / (main / run)).toTask(s" ${(Arduino / portsArgs).value} $args")
  }
}.evaluated

lazy val models = (project in file("./models"))
  .settings(
    libraryDependencies ++= db ++ Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value),
    Compile / managedResourceDirectories += confDir
  )
lazy val confDir = file("./config")
lazy val migrations = (project in file("./migrations"))
  .dependsOn(models)
  .settings (
    libraryDependencies ++= db ++ logs ++ akka ++ Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value),

    Compile / managedResourceDirectories += confDir,

    mainClass := Some("com.ilyak.pifarm.migrations.Main")
  )

lazy val main = (project in file("./main"))
  .enablePlugins(JnaeratorPlugin, ArduinoPlugin)
  .settings(

    libraryDependencies ++= akkaFull ++ db ++ logs ++ json ++ cats ++ serial,
    Compile / managedResourceDirectories += confDir,

    resolvers += Resolver.bintrayRepo("jarlakxen", "maven"),

    name := "raspberry-farm",
    mainClass := Some("com.ilyak.pifarm.Main"),

    scalacOptions ++= Seq(
      //"-Xfatal-warnings",
      "-Ypartial-unification"
    ),

    Compile / resourceGenerators += Def.task {
      import scala.sys.process._
      "npm start" !

      val path = s"${(Compile / resourceDirectory).value.getAbsolutePath}/interface"
      val dir = new File(s"$path/web")
      dir.listFiles().toSeq :+ new File(s"$path/index.html")
    },

    Arduino / arduinos := Map(
      "ttyUSB" -> "arduino:avr:nano:cpu=atmega328old",
      "ttyACM" -> "arduino:avr:uno"
    )
  )
  .dependsOn(models)

Global / (Jnaerator / jnaeratorTargets) += JnaeratorTarget(
  headerFile = baseDirectory.value / "lib" / "all.h",
  packageName = "com.ilyak.wiringPi",
  libraryName = "wiringPi",
  extraArgs = Seq(s"-I${(baseDirectory.value / "lib").getCanonicalPath}")
)
Global / (Jnaerator / jnaeratorRuntime) := BridJ
