package org.pi.farm.storage

import doobie.free.driver
import doobie.hikari.HikariTransactor
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor

import zio.{RLayer, Task, TaskLayer, ZIO, ZLayer}
import zio.interop.catz.*

import org.flywaydb.core.Flyway

object DbLayer {
  def migrate: RLayer[DbConfig, Unit] = ZLayer {
    for {
      config <- ZIO.service[DbConfig]
      flyway <- ZIO.attempt {
                  Flyway
                    .configure()
                    .dataSource(config.url, config.user, config.password)
                    .locations("migrations")
                    .mixed(true)
                    .executeInTransaction(true)
                    .validateMigrationNaming(true)
                    .validateOnMigrate(true)
                    .load()
                }
      _      <- ZIO.attempt(flyway.migrate())
    } yield ()
  }

  def transactor: RLayer[DbConfig & Option[LogHandler[Task]], Transactor[Task]] = ZLayer.scoped {
    for {
      config     <- ZIO.service[DbConfig]
      logHandler <- ZIO.service[Option[LogHandler[Task]]]
      ec         <- ZIO.executor.map(_.asExecutionContext)
      xa         <- HikariTransactor
                      .newHikariTransactor[Task](
                        driverClassName = "org.h2.Driver",
                        url = config.url,
                        user = config.user,
                        pass = config.password,
                        connectEC = ec,
                        logHandler = logHandler
                      )
                      .toScopedZIO
    } yield xa
  }

  val noLogHandler: RLayer[Any, Option[LogHandler[Task]]] = ZLayer.succeed(None)

  val live = migrate ++ transactor

}
