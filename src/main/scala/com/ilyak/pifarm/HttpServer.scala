package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default

object HttpServer {
  def start(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) = {

    val route: Route =
      pathSingleSlash {
        actorSystem.log.debug("web/index.html")

        getFromResource("web/index.html")
      } ~ pathPrefix("web") {
        getFromResourceDirectory("web")
      }


    Http().bindAndHandle(route, "localhost", 8080)
  }
}
