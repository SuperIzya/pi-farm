package com.ilyak.pifarm.db
import slick.jdbc.H2Profile.api._

object TableHelper {
  def add[E, T <: TableQuery[Table[E]]](t: T, r: E): DBIO[Int] = t += r

}
