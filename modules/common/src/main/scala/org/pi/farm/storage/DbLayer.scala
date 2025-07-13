package org.pi.farm.storage

import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import zio.{RLayer, Task, TaskLayer, ZIO, ZLayer}
import zio.interop.catz.*

object DbLayer {
  def migrate: RLayer[DbConfig, Unit] = ZLayer {
    for {
      config <- ZIO.service[DbConfig]
      flyway <- ZIO.attempt {
        Flyway
          .configure()
          .dataSource(config.url, config.user, config.password)
          .locations("classpath:migration")
          .load()
      }
      _ <- ZIO.attempt(flyway.migrate())
    } yield ()
  }

  def transactor: RLayer[DbConfig, Transactor[Task]] = ZLayer.scoped {
    for {
      config <- ZIO.service[DbConfig]
      ec     <- ZIO.executor.map(_.asExecutionContext)
      xa     <- HikariTransactor
        .newHikariTransactor[Task](
          "org.postgresql.Driver",
          config.url,
          config.user,
          config.password,
          ec
        )
        .toScopedZIO
    } yield xa
  }

  val live = migrate ++ transactor

}
