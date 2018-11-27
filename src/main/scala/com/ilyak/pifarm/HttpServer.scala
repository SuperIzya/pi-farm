package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.http.javadsl.model.ws.BinaryMessage
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Sink, Source}
import play.api.libs.json.{JsArray, JsString, Json}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps

object HttpServer {
  val interface = "localhost"
  val port = 8080

  def start(implicit actorSystem: ActorSystem,
            arduinos: ArduinoCollection,
            materializer: ActorMaterializer): Future[Http.ServerBinding] = {

    val log = actorSystem.log

    val socketFlow = Flow[Message]
      .flatMapConcat{
        case TextMessage.Strict(msg) => Source.single(msg)
        case TextMessage.Streamed(src) => src.reduce(_ + _)
        case b: BinaryMessage =>
          b.getStreamedData.runWith(Sink.ignore, materializer)
          Source.empty
      }
      .via(arduinos.combinedFlow)
      .throttle(1, 100 millis, 1, ThrottleMode.Shaping)
      .map(TextMessage(_))

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
