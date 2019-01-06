package com.ilyak.pifarm.backend

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.Source
import com.ilyak.pifarm.db.Tables.LightSchedule
import com.ilyak.pifarm.db.{Db, TableHelper, Tables}
import slick.jdbc.H2Profile.api._
import slick.jdbc.H2Profile.backend.Database

import scala.language.higherKinds
import scala.util.{Failure, Success}

object Backend extends Directives {
  import com.ilyak.pifarm.db.LightScheduleJsonProtocol._
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def routes(implicit db: Database, system: ActorSystem): Route = {
    import akka.http.scaladsl.model._
    import StatusCodes.InternalServerError

    val log = system.log

    path("light-schedule") {
      get {
        val source: Source[LightSchedule, NotUsed] = Source.fromPublisher(
          Db.stream {
            val q = for (s <- Tables.LightScheduleTable) yield s
            q.result
          }
        ).mapConcat(_ => _)

        complete(source)
      } ~ post {
          entity(as[LightSchedule]) { r =>
            val future = Db.run {
              TableHelper.add(Tables.LightScheduleTable, r)
            }

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
