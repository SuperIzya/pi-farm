package com.ilyak.pifarm.migrations

import akka.actor.ActorSystem
import slick.jdbc.JdbcBackend.Database

import scala.io.StdIn


object Main extends App {
  implicit val actorSystem = ActorSystem("RaspberryFarm-Migrations")

  implicit val executionContext = actorSystem.dispatcher
  implicit val db = Database.forConfig("farm-db")

  StdIn.readLine()
  db.close()
  actorSystem.terminate()
}
