import Dependencies._
import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ
import JnaeratorPlugin.autoImport.Jnaerator
import com.github.tototoshi.sbt.slick.CodegenPlugin.autoImport.{ slickCodegenJdbcDriver, slickCodegenOutputPackage }
import com.typesafe.config.ConfigFactory
import sbt.Keys.{ libraryDependencies, mainClass, scalaVersion }
import slick.basic.DatabaseConfig
import slick.codegen.SourceCodeGenerator
import slick.jdbc.JdbcProfile
import slick.model.Model

import scala.language.postfixOps

lazy val actualRun = inputKey[Unit]("The actual run task")

lazy val commonSettings = Seq(
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9"),
  version := "0.1",
  scalaVersion := "2.12.7",
  resolvers += Resolver.bintrayRepo("jarlakxen", "maven"),
  resolvers += Resolver.sonatypeRepo("releases"),
  scalacOptions ++= Seq(
    //"-Xfatal-warnings",
    "-Ypartial-unification"
  ),
  libraryDependencies ++= json,
  Runtime / unmanagedResourceDirectories ++= Seq(file("src/main/resources")) ++ 
    file("src/main/resources").listFiles().filter(_.isDirectory).toSeq,
  exportJars := true,
  Runtime / fullClasspath ++= (Compile / fullClasspath).value
)

lazy val raspberry = (project in file("."))
  .enablePlugins(PackPlugin)
  .dependsOn(migrations, common, gpio, servo, temperature)
  .settings(
    name := "raspberry-farm",
    mainClass := Some("com.ilyak.pifarm.Main"),
    libraryDependencies ++= tests ++ Seq(
      "org.flywaydb" % "flyway-core" % "5.2.4",
      "io.github.classgraph" % "classgraph" % "4.8.37"
    ),
    slickCodegenOutputPackage := "com.ilyak.pifarm.io.db",
    Runtime / fork := true,
    Runtime / trapExit := false
  )
  .settings(commonSettings: _*)

val dbConfig = ConfigFactory.parseFile(new File("./src/main/resources/application.conf"))
val slickDb = DatabaseConfig.forConfig[JdbcProfile]("farm.db", dbConfig)
lazy val props = slickDb.config.getConfig("properties")
lazy val dbUrl = props.getString("url")
lazy val dbUser = props.getString("user")
lazy val dbPassword = props.getString("password")
lazy val codeGenSettings = Seq(
  Compile / sourceGenerators += slickCodegen,
  slickCodegenCodeGenerator := generator,
  slickCodegenDriver := slickDb.profile,
  slickCodegenJdbcDriver := slickDb.config.getString("properties.driver"),
  slickCodegenDatabaseUrl := dbUrl,
  slickCodegenDatabaseUser := dbUser,
  slickCodegenDatabasePassword := dbPassword,
  slickCodegenExcludedTables := Seq(
    "flyway_schema_history"
  ),
)

lazy val gpio = (project in file("./gpio"))
  .enablePlugins(JnaeratorPlugin)
  .settings(commonSettings: _*)
  .settings(
    Jnaerator / jnaeratorRuntime := BridJ,
    Jnaerator / jnaeratorTargets += JnaeratorTarget(
      headerFile = baseDirectory.value / ".." / "lib" / "all.h",
      packageName = "com.ilyak.wiringPi",
      libraryName = "wiringPi",
      extraArgs = Seq(s"-I${ (baseDirectory.value / ".." / "lib").getCanonicalPath }")
    ),
    libraryDependencies ++= akka ++ db ++ logs ++ json ++ cats ++ serial ++ Seq(
      "io.github.classgraph" % "classgraph" % "4.8.37"
    )
  )

lazy val migrations = (project in file("./migrations"))
  .enablePlugins(FlywayPlugin)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= db,
    flywayUrl := dbUrl,
    flywayLocations := findAllSQL(file(".")),
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySqlMigrationPrefix := ""
  )

lazy val common = (project in file("./common"))
  .settings(commonSettings: _*)
  .enablePlugins(CodegenPlugin)
  .settings(codeGenSettings: _*)
  .settings(
    libraryDependencies ++= provided(db ++ akka ++ logs ++ cats ++ serial ++ Seq(
      "io.github.classgraph" % "classgraph" % "4.8.37"
    )),

    slickCodegenOutputPackage := "com.ilyak.pifarm.common.db"
  )

lazy val pluginsBin = file("./plugins/bin")

lazy val pluginsSettings = commonSettings ++ Seq(
  libraryDependencies ++= provided(db ++ akka ++ logs ++ cats),
  artifactPath := pluginsBin,
  fork := true,
  parallelExecution := true
)

lazy val basic = (project in file("./plugins/basic"))
  .dependsOn(common)
  .settings(pluginsSettings: _*)

lazy val garden = (project in file("./plugins/garden"))
  .dependsOn(common)
  .settings(pluginsSettings: _*)

lazy val servo = (project in file("./plugins/servo"))
  .dependsOn(common)
  .settings(pluginsSettings: _*)

lazy val temperature = (project in file("./plugins/temperature"))
  .dependsOn(common, servo)
  .settings(pluginsSettings: _*)

lazy val ioBasic = (project in file("./plugins/io-basic"))
  .dependsOn(common)
  .settings(pluginsSettings: _*)


lazy val buildWeb = taskKey[Seq[File]]("generate web ui to resources")
buildWeb := Def.task {
  import scala.sys.process._
  "npm start" !

  val path = s"${ (Compile / resourceDirectory).value.getAbsolutePath }/interface"
  val dir = new File(s"$path/web")
  dir.listFiles().toSeq :+ new File(s"$path/index.html")
}.value

Arduino / arduinos := Map(
  "ttyUSB" -> "arduino:avr:nano:cpu=atmega328old",
  "ttyACM" -> "arduino:avr:uno"
)

val runAll = inputKey[Unit]("run all together (usually as a deamon)")

runAll := Def.inputTaskDyn {
  import sbt.complete.Parsers.spaceDelimited
  val args = spaceDelimited("<args>").parsed
    .foldLeft(" ") {
      _ + " " + _
    }
  Def.taskDyn {
    (migrations / flywayMigrate).value

    buildWeb.value

    (Compile / run).toTask(s" ${ (Arduino / portsArgs).value } $pluginsBin $args")
  }
}.evaluated


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

    override def autoIncLastAsOption: Boolean = true
  }
}
