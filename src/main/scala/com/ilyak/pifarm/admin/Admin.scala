package com.ilyak.pifarm.admin

import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.{Directives, Route}
import com.ilyak.pifarm.db.Tables.LightSchedule
import com.ilyak.pifarm.db.{TableHelper, Tables}
import com.ilyak.pifarm.utils.SlickPath
import slick.jdbc.H2Profile.api._
import slick.jdbc.H2Profile.backend.Database

import scala.language.higherKinds

object Admin extends Directives with SlickPath {

  import com.ilyak.pifarm.db.LightScheduleJsonProtocol._

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def routes(implicit db: Database, system: ActorSystem): Route = {
    val log = system.log

    path("light-schedule") {
      get {
        completeFromDb {
          Tables.LightScheduleTable.result
        }
      } ~ post {
        entity(as[LightSchedule]) { r =>
          completeFromDb {
            import system.dispatcher
            TableHelper.add(Tables.LightScheduleTable, r).map(_ => r)
          }
        }
      }
    }
  }
}
