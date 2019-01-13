import Dependencies._
import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ
import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.codegen.SourceCodeGenerator
import slick.jdbc.JdbcProfile
import slick.model.Model

import scala.language.postfixOps

version := "0.1"
scalaVersion := "2.12.7"
resolvers += Resolver.bintrayRepo("jarlakxen", "maven")

name := "raspberry-farm"
mainClass := Some("com.ilyak.pifarm.Main")

val dbConfig = ConfigFactory.parseFile(new File("./src/main/resources/application.conf"))
val slickDb = DatabaseConfig.forConfig[JdbcProfile]("farm-db", dbConfig)
lazy val props = slickDb.config.getConfig("db.properties")
lazy val dbUrl = props.getString("url")
lazy val dbUser = props.getString("user")
lazy val dbPassword = props.getString("password")


lazy val migrations = (project in file("./migrations"))
  .enablePlugins(FlywayPlugin)
  .settings(
    libraryDependencies ++= db,
    flywayUrl := dbUrl,
    flywayLocations := Seq("filesystem:./src/main/resources/db"),
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySqlMigrationPrefix := ""
  )

lazy val sdk = (project in file("./sdk"))
  .settings(
    libraryDependencies ++= db ++ akka ++ logs ++ cats
  )

lazy val schedule = (project in file("./plugins/schedule"))
  .dependsOn(sdk)
  .settings(
    libraryDependencies ++= db ++ akka ++ logs,
    artifactPath := file("./plugins/bin"),
    fork := true,
    parallelExecution := true,
  )

scalacOptions ++= Seq(
  //"-Xfatal-warnings",
  "-Ypartial-unification"
)

Jnaerator / jnaeratorTargets += JnaeratorTarget(
  headerFile = baseDirectory.value / "lib" / "all.h",
  packageName = "com.ilyak.wiringPi",
  libraryName = "wiringPi",
  extraArgs = Seq(s"-I${(baseDirectory.value / "lib").getCanonicalPath}")
)

Jnaerator / jnaeratorRuntime := BridJ
libraryDependencies ++= akka ++ db ++ logs ++ json ++ cats ++ serial ++ Seq(
  "org.clapper" %% "classutil" % "1.4.0"
)

lazy val buildWeb = taskKey[Seq[File]]("generate web ui to resources")
buildWeb := Def.task {
  import scala.sys.process._
  "npm start" !

  val path = s"${(Compile / resourceDirectory).value.getAbsolutePath}/interface"
  val dir = new File(s"$path/web")
  dir.listFiles().toSeq :+ new File(s"$path/index.html")
}.value

Arduino / arduinos := Map(
  "ttyUSB" -> "arduino:avr:nano:cpu=atmega328old",
  "ttyACM" -> "arduino:avr:uno"
)

enablePlugins(JnaeratorPlugin, ArduinoPlugin, CodegenPlugin)

dependsOn(migrations, sdk)

val runAll = inputKey[Unit]("run all together (usually as a deamon)")

runAll := Def.inputTaskDyn {
  import sbt.complete.Parsers.spaceDelimited
  val args = spaceDelimited("<args>").parsed
    .foldLeft(" ") {
      _ + " " + _
    }
  Def.taskDyn {
    (Arduino / upload).value
    (migrations / flywayMigrate).value

    buildWeb.value

    (Compile / run).toTask(s" ${(Arduino / portsArgs).value} $args")
  }
}.evaluated


slickCodegenOutputPackage := "com.ilyak.pifarm.db"
slickCodegenDriver := slickDb.profile
slickCodegenJdbcDriver := slickDb.config.getString("db.properties.driver")
slickCodegenDatabaseUrl := dbUrl
slickCodegenDatabaseUser := dbUser
slickCodegenDatabasePassword := dbPassword
slickCodegenExcludedTables := Seq(
  "flyway_schema_history"
)

lazy val generator: Model => SourceCodeGenerator = model => new SourceCodeGenerator(model) {
  override def code: String =
    """
      |import org.joda.time._
      |import com.github.tototoshi.slick.H2JodaSupport._
    """.stripMargin + super.code

  override def entityName: String => String = _.toCamelCase

  override def tableName: String => String = _.toCamelCase + "Table"

  override def Table = new Table(_) {
    override def Column = new Column(_) {
      override def rawType: String = model.tpe match {
        case "java.sql.Time" => "LocalTime"
        case "java.sql.Timestamp" => "DateTime"
        case _ => super.rawType
      }
    }
  }
}

slickCodegenCodeGenerator := generator

Compile / sourceGenerators += slickCodegen
