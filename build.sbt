import JnaeratorPlugin.autoImport.Jnaerator
import JnaeratorPlugin.autoImport.Jnaerator.Runtime.BridJ

name := "raspberry-farm"

version := "0.1"

scalaVersion := "2.12.7"


val akkaVersion = "2.5.17"
val catsVersion = "1.4.0"

Jnaerator.settings

jnaVersion := "4.2.1"
bridjVersion := "0.7.0"

jnaeratorTargets := Seq(
  Jnaerator.Target(
    headerFile = baseDirectory.value / "lib" / "all.h",
    packageName = "com.ilyak.wiringPi",
    libraryName = "wiringPi",
    extraArgs = Seq(s"-I${(baseDirectory.value / "lib").getCanonicalPath}")
  )
)

jnaeratorRuntime := BridJ

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion % Test
)


