import sbt.*

object Dependencies {

  object Versions {
    val zio = "2.1.19"
    val zioJson = "0.7.44"
    val zioLogging = "2.5.0"
    val zioConfig = "4.0.4"
    val slf4j = "2.0.17"
  }

  val serverDependencies = Seq(
    "dev.zio" %% "zio" % Versions.zio,
    "dev.zio" %% "zio-streams" % Versions.zio,
    "dev.zio" %% "zio-json" % Versions.zioJson,
    "dev.zio" %% "zio-logging" % Versions.zioLogging,
    "dev.zio" %% "zio-logging-slf4j" % Versions.zioLogging,
    "dev.zio" %% "zio-config" % Versions.zioConfig,
    "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig,
    "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
    "org.slf4j" % "slf4j-api" % Versions.slf4j,
    "org.slf4j" % "jul-to-slf4j" % Versions.slf4j,
    "dev.zio" %% "zio-test" % Versions.zio % Test,
    "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
  )
}
