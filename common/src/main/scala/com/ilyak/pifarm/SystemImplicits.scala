package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.Config
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

case class SystemImplicits(
  actorSystem: ActorSystem,
  actorMaterializer: Materializer,
  config: Config,
  db: Database,
  dbProfile: JdbcProfile
)
