package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.ActorMaterializer
import play.api.libs.json.{JsArray, JsString, Json}

import scala.concurrent.Future

object HttpServer {
  def start(implicit actorSystem: ActorSystem,
            arduinos: ArduinoCollection,
            materializer: ActorMaterializer): Future[Http.ServerBinding] = {

    val route: Route =
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~ pathPrefix("web") {
        getFromResourceDirectory("web")
      } ~ path("boards") {
        val boards = JsArray(arduinos.broadcasters.keys.map(JsString).toArray)
        val res = Json.asciiStringify(boards)
        complete(res)
      } ~ pathPrefix("board" / RemainingPath) { board =>
        complete(board.toString)
      }


    Http().bindAndHandle(route, "localhost", 8080)
  }

}
