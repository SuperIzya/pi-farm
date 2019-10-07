package com.ilyak.pifarm

import akka.http.scaladsl.Http
import cats.effect.{ ExitCode, IO, IOApp, Resource }
import cats.implicits._
import com.ilyak.pifarm.io.http.HttpServer
import com.typesafe.config.{ Config, ConfigFactory }
import org.flywaydb.core.Flyway

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends IOApp {
  def capitalize(s: String): String = s"${ s(0).toUpper }${ s.slice(1, s.length) }"

  val readLn = IO { scala.io.StdIn.readLine }
  val printLn: String => IO[Unit] = s => IO { println(s) }
  def wrapPrint[T](name: String, aName: String)(action: => IO[T]): IO[T] =
    printLn(s"${ capitalize(aName) }ing $name...") *>
      action <*
      printLn(s"${ capitalize(name) } ${ aName }ed")

  def getConfig: Resource[IO, Config] = Resource.liftF(IO(ConfigFactory.load()))

  def getSystem(config: Config): Resource[IO, Default.System] = Resource.make {
    wrapPrint("akka", "start") { IO(Default.System(config)) }
  } {
    s => wrapPrint("akka", "terminat"){
        IO.fromFuture(IO(Default.System.terminate(s)))
    } map { _ => () }
  }

  def getDb(system: Default.System): Resource[IO, Default.Db] = Resource.make {
    wrapPrint("db", "start") { IO(Default.Db(system.config)) }
  } {
    db => wrapPrint("db", "clos"){ IO { Default.Db.terminate(db) } }
  }

  def getServer(system: Default.System,
                db: Default.Db,
                actors: Default.Actors): Resource[IO, Http.ServerBinding] =
    Resource.make {
      wrapPrint("http", "start") {
        IO.fromFuture(IO(HttpServer("0.0.0.0", 8080, actors.socket, system).start))
      }
    } {
      s => wrapPrint("http", "terminat") {
          IO.fromFuture(IO(s.terminate(1 second)))
      } map { _ => () }
    }

  def runBrowser(isProd: Boolean): IO[Unit] = {
    import scala.sys.process._
    if (isProd) IO { "xdg-open http://localhost:8080" ! }
    else IO { () }
  }

  def migrate(config: Config, system: Default.System): Resource[IO, Unit] = {
    val c = config.getConfig("farm.db.properties")
    val fl: Flyway = Flyway.configure()
      .dataSource(c.getString("url"), c.getString("user"), c.getString("password"))
      .load()
    Resource.liftF(IO(fl.migrate()).map(_ => ())
      .onError {
        case e: Throwable => IO(system.actorSystem.log.error(e.getMessage))
      }
    )
  }

  def run(args: List[String]): IO[ExitCode] = {
    val resources = for {
      config <- getConfig
      system <- getSystem(config)
      _ <- migrate(config, system)
      db <- getDb(system)
      locator = Default.Locator(system, db)
      actors = Default.Actors(system, db, locator)
      server <- getServer(system, db, actors)
    } yield (config, system, db, server, actors, locator)

    resources.use { _ =>
      for {
        _ <- runBrowser(args.nonEmpty)
        _ <- readLn *> readLn *> IO { println("Initiating shutdown...") }
        s <- IO(ExitCode.Success)
      } yield s
    }
  }
}
