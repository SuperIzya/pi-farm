package com.ilyak.pifarm.io.http

import akka.actor.ActorSystem
import akka.http.javadsl.model.ws.BinaryMessage
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import com.ilyak.pifarm.control.admin.Admin
import com.ilyak.pifarm.io.device.ArduinoCollection
import slick.jdbc.H2Profile.backend.Database
import spray.json.{JsArray, JsString}

import scala.language.postfixOps

class HttpServer private(interface: String, port: Int)
                        (implicit actorSystem: ActorSystem,
                         arduinos: ArduinoCollection,
                         materializer: ActorMaterializer,
                         db: Database)
  extends akka.http.scaladsl.server.Directives
    with akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport {

  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  import scala.concurrent.duration._

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  val log = actorSystem.log
  val socketFlow = Flow[Message]
    .flatMapConcat {
      case TextMessage.Strict(msg) => Source.single(msg)
      case TextMessage.Streamed(src) => src.reduce(_ + _)
      case b: BinaryMessage =>
        b.getStreamedData.runWith(Sink.ignore, materializer)
        Source.empty
    }
    .log("ws-in")
    .filter(_ != "beat")
    .via(arduinos.combinedFlow)
    .throttle(3, 500 milliseconds, 1, ThrottleMode.Shaping)
    .map(TextMessage(_))

  val routes: Route = cors() {
    get {
      pathSingleSlash {
        getFromResource("interface/index.html")
      } ~ pathPrefix("web") {
        getFromResourceDirectory("interface/web")
      } ~ path("boards") {
        complete {
          JsArray(arduinos.broadcasters.keys.map(JsString(_)).toVector)
        }
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
    } ~ path("admin") {
      Admin.routes
    }
  }

  def start = Http().bindAndHandle(routes, interface, port)

}

object HttpServer {

  def apply(interface: String, port: Int)
           (implicit actorSystem: ActorSystem,
            arduinos: ArduinoCollection,
            materializer: ActorMaterializer,
            db: Database): HttpServer =
    new HttpServer(interface, port)

}
