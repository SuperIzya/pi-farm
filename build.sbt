import scala.language.postfixOps

import Dependencies.*

lazy val runGen = taskKey[Unit]("Run server with generated test data")

scalaVersion := "3.8.4"
Test / fork  := true
scalafmtOnCompile := true

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf8",
  "-feature",
  "-unchecked",
  "-explain",
  "-experimental",
  "-java-output-version:21"
)
javaOptions ++= Seq(
  "-Xmx2G",
  "-Xms1G",
  "-XX:+UseG1GC",
  "-XX:+UseStringDeduplication",
  "--enable-native-access=ALL-UNNAMED"
)


lazy val root = (project in file("."))
  .aggregate(common, server, commonPlugins)
  .settings(
    name         := "PiFarm",
    version      := "0.1.0",
    organization := "org.pi.farm",
    licenses += ("MIT", uri("https://opensource.org/license/mit/"))
  )

lazy val common = project
  .in(file("modules/common"))
  .settings(
    libraryDependencies ++= commonDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val server = project
  .in(file("modules/server"))
  .enablePlugins(PackPlugin)
  .settings(
    libraryDependencies ++= serverDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    run / fork                 := true,
    Compile / mainClass        := Some("org.pi.farm.Main"),
    packMain                   := Map("PiFarm" -> "org.pi.farm.Main"),
    packGenerateWindowsBatFile := false,

    // in server settings:
    
    runGen := Def.uncached {
      (Test / runMain).toTask(" org.pi.farm.GenMain").value
    }
  )
  .dependsOn(common % "compile->compile;test->test", commonPlugins)

lazy val commonPlugins = project
  .in(file("modules/common-plugins"))
  .settings(
    libraryDependencies ++= commonDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(common % "compile->compile;test->test")
