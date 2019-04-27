package com.ilyak.pifarm.io.http

import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.{ Directives, Route, StandardRoute }
import akka.stream.scaladsl.Source
import com.ilyak.pifarm.io.db.Db
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.OFormat
import slick.dbio.{ DBIO, StreamingDBIO }

import scala.util.{ Failure, Success }
import slick.jdbc.JdbcBackend.Database

trait SlickPath extends Directives with PlayJsonSupport {

  def streamCompleteFromDb[T: OFormat](query: => StreamingDBIO[_, T])
                                      (implicit db: Database,
                                       jsonStreamingSupport: JsonEntityStreamingSupport): StandardRoute =
    complete {
      Source.fromPublisher[T](
        Db.stream { query }
      ).map(implicitly[OFormat[T]].writes)
    }

  def completeFromDb[T: OFormat](query: => DBIO[T])
                                (implicit db: Database): Route = {
    onComplete(Db.run(query)) {
      case Success(value) => complete(value)
      case Failure(ex) => complete(InternalServerError -> ex.getMessage)
    }
  }
}
