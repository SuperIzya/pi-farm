package com.ilyak.pifarm.io.http

import java.util.jar.JarFile

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{ EntityStreamingSupport, JsonEntityStreamingSupport }
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage, UpgradeToWebSocket }
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, StatusCodes, headers }
import akka.http.scaladsl.server.{ RejectionHandler, Route }
import akka.stream.scaladsl.{ Flow, Sink, Source, StreamConverters }
import akka.stream.{ ActorMaterializer, ThrottleMode }
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.ilyak.pifarm.flow.actors.SocketActor
import com.ilyak.pifarm.flow.actors.SocketActor.SocketActors
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future
import scala.language.postfixOps

class HttpServer private(interface: String, port: Int, socket: SocketActors)
                        (implicit actorSystem: ActorSystem,
                         materializer: ActorMaterializer,
                         db: Database)
  extends akka.http.scaladsl.server.Directives
    with PlayJsonSupport {

  import StatusCodes._
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
    .via(SocketActor.flow(socket))
    .throttle(50, 500 milliseconds, 1, ThrottleMode.Shaping)
    .log("ws-out")
    .map(TextMessage(_))

  private val corsResponseHeaders = List(
    headers.`Access-Control-Allow-Origin`.*,
    headers.`Access-Control-Allow-Credentials`(true),
    headers.`Access-Control-Allow-Headers`(
      "Authorization",
      "Content-Type",
      "X-Requested-With",
      "Access-Control-Allow-Origin"
    )
  )
  val settings = CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginMatcher.*)
  val routes: Route = handleRejections(
    RejectionHandler.newBuilder()
      .handleNotFound(path(Remaining) { req =>
        log.error(s"Not found $req")
        complete((NotFound, s"$req not found!!"))
      } )
      .result()
  ) {
    handleRejections(corsRejectionHandler) {
      cors(settings) {
        get {
          pathSingleSlash {
            getFromResource("interface/index.html")
          } ~ pathPrefix("web") {
            getFromResourceDirectory("interface/web")
          } ~ path("api" / "get-plugin" / "file:" ~ Remaining) { req =>
            log.error(s"Requested plugin bundle $req")
            val src = StreamConverters.fromInputStream(() => {
              val arr = req.split("!/")
              val file = new JarFile(arr(0))
              val entry = file.getEntry(arr(1))
              file.getInputStream(entry)
            })

            val entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, src)
            complete(entity)
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
    }
  }


  def start: Future[Http.ServerBinding] = Http().bindAndHandle(routes, interface, port)
}

object HttpServer {

  def apply(interface: String, port: Int, socket: SocketActors)
           (implicit actorSystem: ActorSystem,
            materializer: ActorMaterializer,
            db: Database): HttpServer =
    new HttpServer(interface, port, socket)
}
