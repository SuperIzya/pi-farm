import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ

import scala.language.postfixOps

lazy val akkaVersion = "2.5.18"

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
libraryDependencies ++= Seq(
  "com.github.jarlakxen" %% "reactive-serial" % "1.4",
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.play" %% "play-json" % "2.6.10",
  "ch.megard" %% "akka-http-cors" % "0.3.1",
  "org.typelevel" %% "cats-core" % "1.0.0",
/*
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.h2database" % "h2" % "1.4.197"
*/
)

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

enablePlugins(JnaeratorPlugin, ArduinoPlugin)

val runAll = inputKey[Unit]("run all together (usually as a deamon)")

runAll := Def.inputTaskDyn {
  import sbt.complete.Parsers.spaceDelimited
  val args =  spaceDelimited("<args>").parsed
      .foldLeft(" "){ _ + " " + _ }
  Def.taskDyn {
    (Arduino / upload).value
    Def.task{
      Thread.sleep(1000)
    }.value
    (Compile / run).toTask(s" ${(Arduino / portsArgs).value} $args")
  }
}.evaluated
