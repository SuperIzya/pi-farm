package com.ilyak.pifarm.db

import slick.dbio.{DBIO, StreamingDBIO}
import slick.jdbc.H2Profile.backend.Database

object Db {

  def run[S](a: => DBIO[S])(implicit db: Database) =
      db.run(a)

  def stream[T, S <: StreamingDBIO[_, T]](a: => S)(implicit db: Database) =
    db.stream(a)
}
