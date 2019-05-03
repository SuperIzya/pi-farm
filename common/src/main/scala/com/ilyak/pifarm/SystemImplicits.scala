package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

case class SystemImplicits(
  actorSystem: ActorSystem,
  actorMaterializer: ActorMaterializer,
  config: Config,
  db: Database,
  dbProfile: JdbcProfile
)
