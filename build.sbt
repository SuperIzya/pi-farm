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
libraryDependencies ++= akka ++ db ++ logs ++ json ++ cats ++ serial

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

dependsOn(migrations)

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
  override def entityName: String => String = _.toCamelCase

  override def tableName: String => String = _.toCamelCase + "Table"
}

slickCodegenCodeGenerator := generator

Compile / sourceGenerators += slickCodegen
