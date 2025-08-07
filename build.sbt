import Dependencies.*

import scala.language.postfixOps

inThisBuild(
  Seq(
    scalaVersion := "3.7.1"
  )
)

lazy val root = (project in file("."))
  .aggregate(common, server, model)
  .settings(
    name         := "PiFarm",
    version      := "0.1.0",
    organization := "org.pi.farm",
    licenses += ("MIT", url("https://opensource.org/license/mit/"))
  )

lazy val common = project
  .in(file("modules/common"))
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(model)

lazy val server = project
  .in(file("modules/server"))
  .settings(
    libraryDependencies ++= serverDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    run / fork          := true,
    test / fork         := true,
    Compile / mainClass := Some("org.pi.farm.server.Main")
  )
  .dependsOn(common)

lazy val model = project.in(file("modules/model"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json" % Versions.zioJson
    )
  )
