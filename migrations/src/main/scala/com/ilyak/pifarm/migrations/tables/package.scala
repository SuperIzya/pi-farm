package com.ilyak.pifarm.migrations

import com.ilyak.pifarm.migrations.tables.MigrationsTable.MigrationsTable
import slick.lifted.TableQuery

package object tables {

  case class MigrationRecord(name: String, order: Int)
  val AppliedMigrations = TableQuery[MigrationsTable]
}
