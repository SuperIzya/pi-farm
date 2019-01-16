package com.ilyak.pifarm.db
import slick.jdbc.H2Profile.api._

object TableHelper {
  def add[E, T <: Table[E]](q: TableQuery[T], r: E): DBIO[Int] = q += r
}
