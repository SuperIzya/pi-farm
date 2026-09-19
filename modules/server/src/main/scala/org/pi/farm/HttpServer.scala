package org.pi.farm

import org.pi.farm.model.Types.{*, given}
import org.pi.farm.runtime.*
import org.pi.farm.service.{StaticService, StorageService}
import org.pi.farm.utils.ConfigCompanion
import org.pi.farm.ws.{Command, Data, WSProcessor}

import zio.*
import zio.http.*
import zio.http.Header.HeaderType
import zio.http.Method.{GET, POST}
import zio.json.*
import zio.schema.codec.{BinaryCodec, DecodeError}
import zio.stream.{ZPipeline, ZStream}

import java.nio.file.{Path => JPath}
import scala.language.implicitConversions

class HttpServer(
  serializationService: StorageService,
  staticService: StaticService,
  inbound: SignalHub,
  outbound: ResponseQueue,
  scope: Scope,
  wsProcessor: WSProcessor,
  counter: Ref[Long]
) {

  private final val serializationMap: Map[String, Int => StorageService.EntryStream[Some]] = Map(
    "periphery-type"  -> (x => serializationService.exportPeripheryType(x)),
    "controller-type" -> (x => serializationService.exportControllerType(x)),
    "controller"      -> (x => serializationService.exportController(x)),
    "configuration"   -> (x => serializationService.exportConfiguration(x))
  )

  import HttpServer.binaryCodec

  val routes: Routes[Scope, Response] = Routes(
    Chunk.fromIterable(serializationMap.map {
      case (name, exportFunc) =>
        GET / "api" / "export" / name / int("id") -> handler { (id: Int, request: Request) =>
          import StorageService.*

          val bytes = exportFunc(id).compress
          Response(
            status = Status.Ok,
            headers = Headers(
              Header.ContentDisposition.Attachment(Some(s"$name.tar.gz")),
              Header.ContentType(MediaType.application.`gzip`)
            ),
            body = Body.fromStream(bytes)(using binaryCodec)
          )
        }
    })
  ) ++ Routes(
    POST / "api" / "import"   -> handler { uploadData },
    GET / "images" / trailing -> handler { (path: Path, request: Request) =>
      staticService.getStaticResource(JPath.of("images").resolve(path.toString)).map {
        case (length, data) =>
          Response(
            status = Status.Ok,
            headers = Headers(
              Header.ContentLength(length.toLong),
              Header.ContentType(MediaType.image.png)
            ),
            body = Body.fromStream(data)(using binaryCodec)
          )
      }
    },
    GET / "ws"                -> handler(socket.toResponse),
    GET / trailing            -> Handler.fromFunctionHandler[(Path, Request)] {
      case (path, request) =>
        val fileName = if (path.nonEmpty && path.toString.contains(".")) path else "index.html"
        Handler.fromResource(s"ui/$fileName").contramap(_._2)
    }
  ).sandbox

  private val annotation = zio
    .logging
    .LogAnnotation[Long](
      name = "ws command",
      combine = (_, i) => i,
      render = _.toString
    )

  private def uploadData(req: Request): IO[Response, Response] = {
    if (req.header(Header.ContentType).exists(_.mediaType == MediaType.multipart.`form-data`))
      for {
        _    <- ZIO.debug("Starting to read multipart/form stream")
        form <- req
                  .body
                  .asMultipartFormStream
                  .mapError(ex =>
                    Response(
                      Status.InternalServerError,
                      body = Body.fromString(s"Failed to decode body as multipart/form-data (${ex.getMessage}")
                    )
                  )

        _ <- form
               .fields
               .tap(f => ZIO.log(s"started reading new field: ${f.name}"))
               .mapZIO {
                 case sb: FormField.StreamingBinary =>
                   serializationService
                     .importData(sb.data)
                     .tapError(err => ZIO.logError(s"Failed to import data: $err"))
                 case _                             =>
                   ZIO.unit
               }
               .runDrain
               .mapError(ex =>
                 Response(
                   Status.InternalServerError,
                   body = Body.fromString(s"Failed to process multipart/form-data field (${ex.getMessage})")
                 )
               )

        _ <- ZIO.debug(s"Finished reading multipart/form stream")
      } yield Response.text("OK")
    else ZIO.succeed(Response(status = Status.NotFound))
  }

  private def socket: WebSocketApp[Scope] = Handler
    .webSocket { channel =>
      def sendFrame(frame: WebSocketFrame): Task[Unit] =
        channel.send(ChannelEvent.read(frame))

      ZIO.logInfo("WebSocket connected") *>
        wsProcessor
          .init
          .flatMap(_.foreach(sendFrame))
          .forkIn(scope) *>
        inbound
          .subscribe
          .flatMap {
            _.foreach(in => sendFrame(WebSocketFrame.text(in.toJson)))
          }
          .forkIn(scope) *>
        channel.receiveAll {
          case ChannelEvent.ExceptionCaught(cause) =>
            ZIO.logError(s"WebSocket exception caught: $cause") *> channel.shutdown

          case ChannelEvent.Read(WebSocketFrame.Text(message))         =>
            counter.updateAndGet(_ + 1).flatMap { id =>
              ZIO.logSpan("WS command") {
                val action = for {
                  _   <- ZIO.logDebug(s"Processing ws command: $message")
                  cmd <- ZIO.fromEither(message.fromJson[Command])
                  _   <- wsProcessor.process(cmd).foreach(sendFrame)
                } yield ()

                action.catchAll { e =>
                  val error = s"Failed to processing command `$message`: $e"
                  ZIO.logError(error) *>
                    wsProcessor
                      .splitIfNeeded(Data.error(error).toJson)
                      .flatMap(_.foreach(sendFrame).ignore)
                } @@ annotation(id)
              }
            }
          case ChannelEvent.Read(WebSocketFrame.Ping)                  =>
            channel.send(ChannelEvent.read(WebSocketFrame.pong))
          case ChannelEvent.Read(WebSocketFrame.Close(status, reason)) =>
            channel.shutdown *>
              ZIO.logInfo(s"WebSocket closed with status: $status, reason: $reason")
          case _                                                       => ZIO.unit
        }
    }
    .tapErrorCauseZIO(ZIO.logErrorCause("Error in websocket", _))

}

object HttpServer {
  type Env = SignalHub & ResponseQueue & Scope & WSProcessor & Server & UIIncomingQueue & StorageService & StaticService

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound              <- ZIO.service[SignalHub]
      outbound             <- ZIO.service[ResponseQueue]
      scope                <- ZIO.service[Scope]
      wsProcessor          <- ZIO.service[WSProcessor]
      serializationService <- ZIO.service[StorageService]
      staticService        <- ZIO.service[StaticService]
      counter              <- Ref.make(0L)
      server                = new HttpServer(serializationService, staticService, inbound, outbound, scope, wsProcessor, counter)
      _                    <- server.routes.serve.forkScoped
      _                    <- ZIO.logInfo(s"HTTP server started")
    } yield ()
  }

  case class Config(port: Int)

  object Config extends ConfigCompanion[Config]("http-server")

  private final val binaryCodec: BinaryCodec[Byte] = new BinaryCodec[Byte] {

    val streamDecoder: ZPipeline[Any, DecodeError, Byte, Byte] = ZPipeline.identity

    val streamEncoder: ZPipeline[Any, Nothing, Byte, Byte] = ZPipeline.identity

    def encode(a: Byte): Chunk[Byte] = Chunk.single(a)

    def decode(chunk: Chunk[Byte]): Either[DecodeError, Byte] =
      Either.cond(chunk.size == 1, chunk(0), DecodeError.ReadError(Cause.empty, "Expected a single byte"))

  }
}
