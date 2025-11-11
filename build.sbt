import Dependencies.*

import scala.language.postfixOps

inThisBuild(
  Seq(
    scalaVersion := "3.7.1",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "utf8",
      "-feature",
      "-unchecked",
      "-explain",
      "-experimental"
    ),
    scalafmtOnCompile := true
  )
)

lazy val root = (project in file("."))
  .aggregate(common, server, commonPlugins)
  .settings(
    name         := "PiFarm",
    version      := "0.1.0",
    organization := "org.pi.farm",
    licenses += ("MIT", url("https://opensource.org/license/mit/"))
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
    test / fork                := true,
    Compile / mainClass        := Some("org.pi.farm.Main"),
    packMain                   := Map("PiFarm" -> "org.pi.farm.Main"),
    packGenerateWindowsBatFile := false /*,
    packEnvVars ++= Map(
      "HTTP_PORT": "80",
      "UDP_PORT": "90"
    )*/
  )
  .dependsOn(common % "compile->compile;test->test")

lazy val commonPlugins = project
  .in(file("modules/common-plugins"))
  .settings(
    libraryDependencies ++= commonDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(common % "compile->compile;test->test")
