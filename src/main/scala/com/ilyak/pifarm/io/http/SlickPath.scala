package com.ilyak.pifarm.io.http

import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.{Directives, Route, StandardRoute}
import akka.stream.scaladsl.Source
import com.ilyak.pifarm.io.db.Db
import slick.dbio.{DBIO, StreamingDBIO}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.util.{Failure, Success}
import slick.jdbc.H2Profile.backend.Database

trait SlickPath extends Directives with DefaultJsonProtocol with SprayJsonSupport {

  def streamCompleteFromDb[T](query: => StreamingDBIO[_, T])
                             (implicit db: Database,
                              format: RootJsonFormat[T],
                              jsonStreamingSupport: JsonEntityStreamingSupport): StandardRoute =
    complete {
      Source.fromPublisher[T](
        Db.stream {
          query
        }
      ) map format.write
    }

  def completeFromDb[T](query: => DBIO[T])
                       (implicit db: Database,
                        format: RootJsonFormat[T]): Route = {
    onComplete(Db.run(query)) {
      case Success(value) => complete(value)
      case Failure(ex) => complete(InternalServerError -> ex.getMessage)
    }
  }

}
