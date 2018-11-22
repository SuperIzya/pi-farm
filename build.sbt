import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ

lazy val catsVersion = "1.4.0"
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
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion % Test,
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
)

Compile / resourceGenerators += Def.task {
  (Compile / webpack).toTask("").value
  new File((Compile / resourceDirectory).value.getAbsolutePath + "/web").listFiles().toSeq
}


enablePlugins(JnaeratorPlugin, ArduinoPlugin, SbtWeb)


