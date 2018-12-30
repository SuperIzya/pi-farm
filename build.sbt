import scala.language.postfixOps
import Dependencies._
import MainProject._

lazy val models = (project in file("./models"))
  .settings(
    libraryDependencies ++= db,
    managedResourceDirectories += confDir
  )
lazy val confDir = file("./config")
lazy val migrations = (project in file("./migrations"))
  .dependsOn(models)
  .settings(
    libraryDependencies ++= db ++ logs ++ akka,
    managedResourceDirectories += confDir
  )

lazy val main = mainProject
  .dependsOn(models)
  .settings(
    libraryDependencies ++= akkaFull ++ db ++ logs ++ json ++ cats ++ serial,
    managedResourceDirectories += confDir
  )

val runAll = inputKey[Unit]("run all together")

runAll := Def.inputTaskDyn {
  import sbt.complete.Parsers.spaceDelimited
  val args = spaceDelimited("<args>").parsed
    .foldLeft(" ") {
      _ + " " + _
    }
  Def.taskDyn {
    (Arduino / upload).value
    (Compile / (migrations / run)).toTask("").value
    (Compile / (main / run)).toTask(s" ${(Arduino / portsArgs).value} $args")
  }
}.evaluated

