package com.ilyak.pifarm

import akka.actor.ActorSystem
import akka.http.javadsl.model.ws.BinaryMessage
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.ilyak.pifarm.monitor.Monitor
import com.ilyak.pifarm.shapes.ShapeHelper
import play.api.libs.json.{JsArray, JsString, Json}

import scala.language.postfixOps

class HttpServer private(interface: String, port: Int)
                        (implicit actorSystem: ActorSystem,
                         monitor: Monitor,
                         arduinos: ArduinoCollection,
                         materializer: ActorMaterializer) {

  import HttpServer.FlowBits._
  import akka.http.scaladsl.server.Directives._
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  val broadcasters = arduinos.broadcasters
  val log = actorSystem.log
  val broadcastFlow = ShapeHelper.flowThroughAll(broadcasters)
  val socketFlow = messageToString
    .via(broadcastFlow)
    .merge(Monitor.source.map(_.toString))
    //  .throttle(3, 500 milliseconds, 1, ThrottleMode.Shaping)
    .map(TextMessage(_))

  val routes = cors() {
    get {
      pathSingleSlash {
        getFromResource("interface/index.html")
      } ~ pathPrefix("web") {
        getFromResourceDirectory("interface/web")
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
  }

  def start = Http().bindAndHandle(routes, interface, port)
}

object HttpServer {

  def apply(interface: String, port: Int)
           (implicit actorSystem: ActorSystem,
            monitor: Monitor,
            arduinos: ArduinoCollection,
            materializer: ActorMaterializer): HttpServer =
    new HttpServer(interface, port)


  private object FlowBits {
    def messageToString(implicit materializer: ActorMaterializer) =
      Flow[Message]
        .flatMapConcat {
          case TextMessage.Strict(msg) => Source.single(msg)
          case TextMessage.Streamed(src) => src.reduce(_ + _)
          case b: BinaryMessage =>
            b.getStreamedData.runWith(Sink.ignore, materializer)
            Source.empty
        }
        .log("ws-in")
        .filter(_ != "beat")
  }

}
