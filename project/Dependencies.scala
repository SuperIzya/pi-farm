object Dependencies {
  import sbt._
  lazy val akkaVersion = "2.5.18"
  lazy val slickVersion = "3.2.3"

  lazy val db = Seq(
    "com.typesafe.slick" %% "slick" % "3.2.3",
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "com.typesafe.slick" %% "slick-codegen" % slickVersion,
    "com.h2database" % "h2" % "1.4.197"
  )

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-http" % "10.1.5",
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
    "com.typesafe.play" %% "play-json" % "2.6.10",
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "1.0.0",
  )
}