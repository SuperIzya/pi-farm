

object Dependencies {

  import sbt._

  lazy val akkaVersion = "2.5.22"
  lazy val akkaHttpVersion = "10.1.8"
  lazy val akkaHttpCorsVersion = "0.4.0"
  lazy val akkaHttpPlayJsonVersion = "1.25.2"
  lazy val playJsonVersion = "2.7.3"
  lazy val slickVersion = "3.3.0"
  lazy val catsVersion = "1.6.0"
  lazy val kittensVersion = "1.2.1"
  lazy val scalatestVersion = "3.0.7"
  lazy val slf4jVersion = "1.7.25"
  lazy val reactiveSerialVersion = "1.4"
  lazy val logbackVersion = "1.2.3"

  def provided(s: Seq[ModuleID]): Seq[ModuleID] = s map (_ % "provided")

  lazy val db = Seq(
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "com.typesafe.slick" %% "slick-codegen" % slickVersion,
    "com.h2database" % "h2" % "1.4.199",
    "com.github.tototoshi" %% "slick-joda-mapper" % "2.4.0",
    "joda-time" % "joda-time" % "2.10.1",
    "org.joda" % "joda-convert" % "2.2.0"
  )

  lazy val tests = Seq(
    "com.typesafe.slick" %% "slick-testkit" % slickVersion % Test,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  )

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "ch.megard" %% "akka-http-cors" % akkaHttpCorsVersion,
  )

  lazy val logs = Seq(
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "net.logstash.logback" % "logstash-logback-encoder" % "5.3",
  )

  lazy val serial = Seq(
    "com.github.jarlakxen" %% "reactive-serial" % reactiveSerialVersion,
  )

  lazy val json = Seq(
    //"com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
    "de.heikoseeberger" %% "akka-http-play-json" % akkaHttpPlayJsonVersion,
    "com.typesafe.play" %% "play-json" % playJsonVersion
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
