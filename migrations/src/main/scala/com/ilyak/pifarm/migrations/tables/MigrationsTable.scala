package com.ilyak.pifarm.migrations.tables
import slick.jdbc.H2Profile.api._
import slick.lifted


object MigrationsTable {
  case class LiftedMigration(name: Rep[String], order: lifted.Rep[Int])
  implicit object MigrationShape extends CaseClassShape(LiftedMigration.tupled, MigrationRecord.tupled)


  class MigrationsTable(tag: Tag) extends Table[MigrationRecord](tag, "migrations") {
    def name = column[String]("name", O.PrimaryKey)

    def order = column[Int]("order", O.Unique)

    override def * = LiftedMigration(name, order)
  }

}