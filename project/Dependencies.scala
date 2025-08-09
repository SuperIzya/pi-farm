import sbt.*

object Dependencies {

  object Versions {
    val zio           = "2.1.20"
    val zioCats       = "23.1.0.5"
    val zioJson       = "0.7.44"
    val zioLogging    = "2.5.1"
    val zioConfig     = "4.0.4"
    val slf4j         = "2.0.17"
    val zioHttp       = "3.3.3"
    val doobieVersion = "1.0.0-RC10"
    val flywayVersion = "11.11.0"
    val h2Version     = "2.3.232"
    val chimney = "2.0.0-M1"
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
    "org.flywaydb" % "flyway-core" % Versions.flywayVersion,
    "dev.zio"     %% "zio-json"    % Versions.zioJson,

    "dev.zio"  %% "zio-test"            % Versions.zio % Test,
    "dev.zio"  %% "zio-test-sbt"        % Versions.zio % Test
  )

  val serverDependencies = Seq(
    "dev.zio"  %% "zio"                 % Versions.zio,
    "dev.zio"  %% "zio-streams"         % Versions.zio,
    "dev.zio"  %% "zio-http"            % Versions.zioHttp,
    "dev.zio"  %% "zio-json"            % Versions.zioJson,
    "dev.zio"  %% "zio-logging"         % Versions.zioLogging,
    "dev.zio"  %% "zio-logging-slf4j"   % Versions.zioLogging,
    "dev.zio"  %% "zio-config"          % Versions.zioConfig,
    "dev.zio"  %% "zio-config-typesafe" % Versions.zioConfig,
    "dev.zio"  %% "zio-config-magnolia" % Versions.zioConfig,
    "org.slf4j" % "slf4j-api"           % Versions.slf4j,
    "org.slf4j" % "jul-to-slf4j"        % Versions.slf4j,
    "dev.zio"  %% "zio-test"            % Versions.zio % Test,
    "dev.zio"  %% "zio-test-sbt"        % Versions.zio % Test
  )


}
