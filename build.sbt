import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ

import scala.language.postfixOps

lazy val akkaVersion = "2.5.18"

version := "0.1"
scalaVersion := "2.12.7"
resolvers += Resolver.bintrayRepo("jarlakxen", "maven")

name := "raspberry-farm"
mainClass := Some("com.ilyak.pifarm.Main")

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
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "com.typesafe.play" %% "play-json" % "2.6.10",
  "ch.megard" %% "akka-http-cors" % "0.3.1"
)

Compile / resourceGenerators += Def.task {
  import scala.sys.process._
  "npm start" !

  val path = s"${(Compile / resourceDirectory).value.getAbsolutePath}/interface"
  val dir = new File(s"$path/web")
  dir.listFiles().toSeq :+ new File(s"$path/index.html")
}

enablePlugins(JnaeratorPlugin, ArduinoPlugin)


