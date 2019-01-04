package com.ilyak.pifarm.migrations

import com.ilyak.pifarm.macros.migrationsManager

@migrationsManager
object MigrationManager {
  private var migrations: Seq[Migration] = Seq.empty

  private [migrations] def register(m: Migration) = migrations :+= m
}
