package com.ilyak.pifarm.backend

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.Source
import com.ilyak.pifarm.db.Tables.LightSchedule
import com.ilyak.pifarm.db.{Db, TableHelper, Tables}
import play.api.libs.json.Json

import scala.language.higherKinds
import slick.jdbc.H2Profile.api._
import slick.jdbc.H2Profile.backend.Database

import scala.util.{Success, Failure}

object Backend extends Directives {
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
  implicit val lightScheduleSerializer = Json.format[LightSchedule]

  def routes(implicit db: Database, system: ActorSystem): Route = {
    import system.dispatcher
    import akka.http.scaladsl.model._
    import StatusCodes.InternalServerError

    val log = system.log

    path("light-schedule") {
      get {
        val source: Source[String, NotUsed] = Source.fromPublisher[LightSchedule](
          Db.stream {
            Tables.LightScheduleTable.to[Set[LightSchedule]].result
          }
        )
          .map(Json.toJson)
          .map(Json.asciiStringify(_))

        complete(source)
      } ~ post {
          entity(as[LightSchedule]) { r =>
            val future = Db.run {
              TableHelper.add(Tables.LightScheduleTable, r)
            }
              .map(Json.toJson)
              .map(Json.asciiStringify(_))


            onComplete(future) {
              case Success(value) => complete(value)
              case Failure(ex) =>
                log.error("Error while inserting LightScheduleTable:", ex)
                complete((InternalServerError, s"Error while inserting LightScheduleTable: ${ex.getMessage}"))
            }

          }
        }
    }
  }
}
