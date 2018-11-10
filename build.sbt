import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ

name := "raspberry-farm"

version := "0.1"

scalaVersion := "2.12.7"

val catsVersion = "1.4.0"

enablePlugins(JnaeratorPlugin)

JnaeratorConfig / jnaeratorTargets := Seq(
  JnaeratorTarget(
    headerFile = baseDirectory.value / "lib" / "all.h",
    packageName = "com.ilyak.wiringPi",
    libraryName = "wiringPi",
    extraArgs = Seq(s"-I${(baseDirectory.value / "lib").getCanonicalPath}")
  )
)
JnaeratorConfig / jnaeratorRuntime := BridJ

libraryDependencies ++= Seq(
  "com.nativelibs4java" % "bridj" % "0.7.0",
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion % Test
)


