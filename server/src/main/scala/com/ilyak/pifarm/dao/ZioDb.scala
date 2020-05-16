package com.ilyak.pifarm.dao

import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import zio.Task

import scala.language.higherKinds

object ZioDb {
  implicit class toZio[R](val dbio: DBIO[R]) extends AnyVal {
    def toZio(implicit db: Database): Task[R] =
      Task.fromFuture(implicit ex => db.run(dbio))
  }
}

