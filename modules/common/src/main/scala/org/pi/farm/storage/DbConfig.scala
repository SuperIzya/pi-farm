package org.pi.farm.storage

import org.pi.farm.utils.ConfigCompanion

case class DbConfig(url: String, user: String, password: String)

object DbConfig extends ConfigCompanion[DbConfig]("database")
