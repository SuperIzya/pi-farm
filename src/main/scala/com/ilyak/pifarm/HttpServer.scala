package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.{ActorMaterializer, Attributes}
import akka.stream.scaladsl.Flow
import play.api.libs.json.{JsArray, JsString, Json}

import scala.concurrent.Future

object HttpServer {
  val interface = "localhost"
  val port = 8080

  def start(implicit actorSystem: ActorSystem,
            arduinos: ArduinoCollection,
            materializer: ActorMaterializer): Future[Http.ServerBinding] = {

    val log = actorSystem.log

    val socketFlow = Flow[Message]
      .map(_.asTextMessage.getStrictText)
      .via(arduinos.mergedFlow)
      .log("arduino")
      .withAttributes(
        Attributes.logLevels(
          onElement = Logging.WarningLevel,
          onFinish = Logging.InfoLevel,
          onFailure = Logging.DebugLevel
        )
      )
      .map(TextMessage.Strict)

    val routes =
      get {
        pathSingleSlash {
          getFromResource("web/index.html")
        } ~ pathPrefix("web") {
          getFromResourceDirectory("web")
        } ~ path("boards") {
          val boards = JsArray(arduinos.broadcasters.keys.map(JsString).toArray)
          complete(Json.asciiStringify(boards))
        }
      } ~ (path("socket") & extractRequest) {
        _.header[UpgradeToWebSocket] match {
          case Some(upgrade) =>
            log.debug("Starting socket")
            complete(upgrade.handleMessages(socketFlow))
          case None =>
            log.debug("Request for socket failed")
            redirect("/", StatusCodes.TemporaryRedirect)
        }
      }


    Http().bindAndHandle(routes, interface, port)
  }


}
