import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ

name := "raspberry-farm"

version := "0.1"

scalaVersion := "2.12.7"

val catsVersion = "1.4.0"

enablePlugins(JnaeratorPlugin)

Jnaerator / jnaeratorTargets := Seq(
  JnaeratorTarget(
    headerFile = baseDirectory.value / "lib" / "all.h",
    packageName = "com.ilyak.wiringPi",
    libraryName = "wiringPi",
    extraArgs = Seq(s"-I${(baseDirectory.value / "lib").getCanonicalPath}")
  )
)
Jnaerator / jnaeratorRuntime := BridJ
libraryDependencies += (Jnaerator / jnaeratorEngine).value

enablePlugins(ArduinoPlugin)
arduinoSources := baseDirectory.value / "arduino"


libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion % Test
)


