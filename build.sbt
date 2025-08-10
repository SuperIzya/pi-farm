import Dependencies.*

import scala.language.postfixOps

inThisBuild(
  Seq(
    scalaVersion := "3.7.1"
  )
)

lazy val root = (project in file("."))
  .aggregate(common, server)
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
  .settings(
    libraryDependencies ++= serverDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    run / fork          := true,
    test / fork         := true,
    Compile / mainClass := Some("org.pi.farm.Main")
  )
  .dependsOn(common % "compile->compile;test->test")
