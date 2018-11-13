import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ

name := "raspberry-farm"

version := "0.1"

scalaVersion := "2.12.7"

lazy val catsVersion = "1.4.0"
lazy val akkaVersion = "2.5.18"

enablePlugins(JnaeratorPlugin, ArduinoPlugin)

Jnaerator / jnaeratorTargets += JnaeratorTarget(
  headerFile = baseDirectory.value / "lib" / "all.h",
  packageName = "com.ilyak.wiringPi",
  libraryName = "wiringPi",
  extraArgs = Seq(s"-I${(baseDirectory.value / "lib").getCanonicalPath}")
)

Jnaerator / jnaeratorRuntime := BridJ

resolvers += Resolver.bintrayRepo("jarlakxen", "maven")
libraryDependencies ++= Seq(
  "com.github.jarlakxen" %% "reactive-serial" % "1.4",
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion % Test,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)


