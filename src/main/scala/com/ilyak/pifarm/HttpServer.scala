package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.ActorMaterializer

import scala.concurrent.Future

object HttpServer {
  def start(implicit actorSystem: ActorSystem,
            arduinos: Map[String, Arduino],
            materializer: ActorMaterializer): Future[Http.ServerBinding] = {

    val route: Route =
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~ pathPrefix("web") {
        getFromResourceDirectory("web")
      } ~ path("boards") {
        complete(arduinos.size.toString)
      }


    Http().bindAndHandle(route, "localhost", 8080)
  }
}
