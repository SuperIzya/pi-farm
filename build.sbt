import scala.language.postfixOps
import Dependencies.*

inThisBuild(Seq(
  scalaVersion := "3.7.1"
))

lazy val common = project.in(file("modules/common"))

lazy val server = project.in(file("modules/server"))
  .settings(
    libraryDependencies ++= serverDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    run / fork := true,
    test / fork := true,
    Compile / mainClass := Some("org.pi.farm.server.Main")
  )
  .dependsOn(common)
