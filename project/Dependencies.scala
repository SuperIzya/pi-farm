

object Dependencies {

  import sbt._

  lazy val akkaVersion = "2.5.18"
  lazy val akkaHttpVersion = "10.1.7"
  lazy val slickVersion = "3.2.3"
  lazy val catsVersion = "1.5.0"
  lazy val kittensVersion = "1.2.0"

  def provided(s: Seq[ModuleID]): Seq[ModuleID] = s map (_ % "provided")

  lazy val db = Seq(
    "com.typesafe.slick" %% "slick" % "3.2.3",
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "com.typesafe.slick" %% "slick-codegen" % slickVersion,
    "com.h2database" % "h2" % "1.4.197",
    "com.github.tototoshi" %% "slick-joda-mapper" % "2.3.0",
    "joda-time" % "joda-time" % "2.7",
    "org.joda" % "joda-convert" % "1.7"
  )

  lazy val tests = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  )

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "ch.megard" %% "akka-http-cors" % "0.3.1",
  )

  lazy val logs = Seq(
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
  )

  lazy val serial = Seq(
    "com.github.jarlakxen" %% "reactive-serial" % "1.4",
  )

  lazy val json = Seq(
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.typelevel" %% "kittens" % kittensVersion,
  )


  def findAllSQL(project: File): Seq[String] = {
    val re = ".*/src/main/resources/db/.+".r.unanchored

    def run(cur: File, res: String): Seq[String] = {
      val name = if(res.isEmpty) cur.name else res + "/" + cur.name
      if (cur.isDirectory) cur
        .listFiles()
        .map(run(_, name))
        .foldLeft(Seq.empty[String])(_ ++ _)
      else {
        name match {
          case re() => Seq(res)
          case _ => Seq.empty
        }
      }
    }

    run(project, "").map("filesystem:" + _)
  }
}
