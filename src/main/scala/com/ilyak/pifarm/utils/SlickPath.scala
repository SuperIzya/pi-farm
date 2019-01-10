package com.ilyak.pifarm.utils

import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.{Directives, StandardRoute}
import akka.stream.scaladsl.Source
import com.ilyak.pifarm.admin.Admin.{complete, onComplete}
import com.ilyak.pifarm.db.{Db, TableHelper, Tables}
import slick.dbio.{DBIO, StreamingDBIO}
import slick.jdbc.H2Profile.backend.Database
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds
import scala.util.{Failure, Success}

trait SlickPath extends Directives with DefaultJsonProtocol with SprayJsonSupport {

  def completeFromDb[T](query: => StreamingDBIO[_, T])
                       (implicit db: Database,
                        format: RootJsonFormat[T],
                        jsonStreamingSupport: JsonEntityStreamingSupport): StandardRoute =
    complete {
      Source.fromPublisher[T](
        Db.stream {
          query
        }
      )
    }

  def completeFromDb[T](query: => DBIO[T])
                       (implicit db: Database,
                        format: RootJsonFormat[T]): StandardRoute = {
    onComplete(Db.run { query }) {
      case Success(value) => complete(value)
      case Failure(ex) =>
        complete(InternalServerError -> ex.getMessage)
    }
  }

}
