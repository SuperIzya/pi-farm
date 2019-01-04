package com.ilyak.pifarm.migrations

trait Migration {
  MigrationManager.register(this)
}
