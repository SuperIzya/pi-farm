import Dependencies._
import JnaeratorPlugin.JnaeratorTarget
import JnaeratorPlugin.Runtime.BridJ
import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.language.postfixOps

version := "0.1"
scalaVersion := "2.12.7"
resolvers += Resolver.bintrayRepo("jarlakxen", "maven")

name := "raspberry-farm"
mainClass := Some("com.ilyak.pifarm.Main")


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

Compile / resourceGenerators += Def.task {
  import scala.sys.process._
  "npm start" !

  val path = s"${(Compile / resourceDirectory).value.getAbsolutePath}/interface"
  val dir = new File(s"$path/web")
  dir.listFiles().toSeq :+ new File(s"$path/index.html")
}

Arduino / arduinos := Map(
  "ttyUSB" -> "arduino:avr:nano:cpu=atmega328old",
  "ttyACM" -> "arduino:avr:uno"
)

enablePlugins(JnaeratorPlugin, ArduinoPlugin, CodegenPlugin)

val runAll = inputKey[Unit]("run all together (usually as a deamon)")

runAll := Def.inputTaskDyn {
  import sbt.complete.Parsers.spaceDelimited
  val args = spaceDelimited("<args>").parsed
    .foldLeft(" ") {
      _ + " " + _
    }
  Def.taskDyn {
    (Arduino / upload).value
    Def.task {
      Thread.sleep(1000)
    }.value
    (Compile / run).toTask(s" ${(Arduino / portsArgs).value} $args")
  }
}.evaluated


val dbConfig = ConfigFactory.parseFile(new File("./src/main/resources/application.conf"))
val slickDb = DatabaseConfig.forConfig[JdbcProfile]("farm-db", dbConfig)

slickCodegenOutputPackage := "com.ilyak.pifarm.db"
slickCodegenDriver := slickDb.profile
slickCodegenJdbcDriver := slickDb.profileName
slickCodegenDatabaseUrl := slickDb.config.getString("url")

Compile / sourceGenerators += slickCodegen
