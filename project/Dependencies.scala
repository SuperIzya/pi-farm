import sbt.*

object Dependencies {

  object Versions {
    val zio           = "2.1.24"
    val zioCats       = "23.1.0.13"
    val zioJson       = "0.9.0"
    val zioLogging    = "2.5.3"
    val zioConfig     = "4.0.6"
    val slf4j         = "2.0.17"
    val zioHttp       = "3.10.1"
    val doobieVersion = "1.0.0-RC12"
    val flywayVersion = "12.1.0"
    val h2Version     = "2.4.240"
    val chimney       = "1.9.0"
    val logback       = "1.5.32"
  }

  val commonDependencies = Seq(
    "dev.zio" %% "zio-config"          % Versions.zioConfig,
    "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig,
    "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
    "dev.zio" %% "zio-interop-cats"    % Versions.zioCats,
    // Doobie dependencies
    "org.tpolecat" %% "doobie-core"   % Versions.doobieVersion,
    "org.tpolecat" %% "doobie-h2"     % Versions.doobieVersion,
    "org.tpolecat" %% "doobie-hikari" % Versions.doobieVersion,

    // H2 database
    "com.h2database" % "h2" % Versions.h2Version,

    // Flyway for database migrations
    "org.flywaydb"  % "flyway-core"  % Versions.flywayVersion,
    "dev.zio"      %% "zio-json"     % Versions.zioJson,
    "io.scalaland" %% "chimney"      % Versions.chimney,
    "dev.zio"      %% "zio-test"     % Versions.zio % Test,
    "dev.zio"      %% "zio-test-sbt" % Versions.zio % Test
  )

  val serverDependencies = Seq(
    "io.scalaland"  %% "chimney"             % Versions.chimney,
    "dev.zio"       %% "zio"                 % Versions.zio,
    "dev.zio"       %% "zio-streams"         % Versions.zio,
    "dev.zio"       %% "zio-http"            % Versions.zioHttp,
    "dev.zio"       %% "zio-json"            % Versions.zioJson,
    "dev.zio"       %% "zio-config"          % Versions.zioConfig,
    "dev.zio"       %% "zio-config-typesafe" % Versions.zioConfig,
    "dev.zio"       %% "zio-config-magnolia" % Versions.zioConfig,
    "dev.zio"       %% "zio-logging"         % Versions.zioLogging,
    "dev.zio"       %% "zio-logging-slf4j2"  % Versions.zioLogging,
    "ch.qos.logback" % "logback-classic"     % Versions.logback,
    "dev.zio"       %% "zio-test"            % Versions.zio % Test,
    "dev.zio"       %% "zio-test-sbt"        % Versions.zio % Test
  )

}
