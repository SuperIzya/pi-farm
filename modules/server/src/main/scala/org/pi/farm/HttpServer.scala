package org.pi.farm

import org.pi.farm.model.Types.*
import org.pi.farm.runtime.*
import org.pi.farm.service.SerializationService
import org.pi.farm.utils.ConfigCompanion
import org.pi.farm.ws.{Command, Data, WSProcessor}

import zio.*
import zio.http.*
import zio.http.Header.HeaderType
import zio.http.Method.GET
import zio.json.*
import zio.schema.codec.{BinaryCodec, DecodeError}
import zio.stream.{ZPipeline, ZStream}

class HttpServer(
  serializationService: SerializationService,
  inbound: SignalHub,
  outbound: ResponseQueue,
  scope: Scope,
  wsProcessor: WSProcessor,
  counter: Ref[Long]
) {

  private def exportData(
    name: String,
    f: SerializationService => SerializationService.EntryStream[Some]
  ): Response = {
    import SerializationService.*
    import HttpServer.given

    val bytes = f(serializationService).compress
    Response(
      status = Status.Ok,
      headers = Headers(
        Header.ContentDisposition.Attachment(Some(s"$name.tar.gz")),
        Header.ContentType(MediaType.application.`gzip`)
      ),
      body = Body.fromStream(bytes)
    )
  }

  val routes: Routes[Scope, Response] = Routes(
    GET / "api" / "export" / "periphery-type" / int("id")  -> handler { (id: Int, request: Request) =>
      exportData(s"periphery-type", _.exportPeripheryType(id.toPeripheryTypeId))
    },
    GET / "api" / "export" / "controller-type" / int("id") -> handler { (id: Int, request: Request) =>
      exportData(s"controller-type", _.exportControllerType(id.toControllerTypeId))
    },
    GET / "api" / "export" / "controller" / int("id")      -> handler { (id: Int, request: Request) =>
      exportData(s"controller", _.exportController(id.toControllerId))
    },
    GET / "api" / "export" / "configuration" / int("id")   -> handler { (id: Int, request: Request) =>
      exportData(s"configuration", _.exportConfiguration(id.toConfigurationId))
    },
    GET / "ws"                                             -> handler(socket.toResponse),
    GET / trailing                                         -> Handler.fromFunctionHandler[(Path, Request)] {
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
  type Env = SignalHub & ResponseQueue & Scope & WSProcessor & Server & UIIncomingQueue & SerializationService

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound              <- ZIO.service[SignalHub]
      outbound             <- ZIO.service[ResponseQueue]
      scope                <- ZIO.service[Scope]
      wsProcessor          <- ZIO.service[WSProcessor]
      serializationService <- ZIO.service[SerializationService]
      counter              <- Ref.make(0L)
      server                = new HttpServer(serializationService, inbound, outbound, scope, wsProcessor, counter)
      _                    <- server.routes.serve.forkScoped
      _                    <- ZIO.logInfo(s"HTTP server started")
    } yield ()
  }

  case class Config(port: Int)

  object Config extends ConfigCompanion[Config]("http-server")

  given BinaryCodec[Byte] {

    val streamDecoder: ZPipeline[Any, DecodeError, Byte, Byte] = ZPipeline.identity

    val streamEncoder: ZPipeline[Any, Nothing, Byte, Byte] = ZPipeline.identity

    def encode(a: Byte): Chunk[Byte] = Chunk.single(a)

    def decode(chunk: Chunk[Byte]): Either[DecodeError, Byte] =
      Either.cond(chunk.size == 1, chunk(0), DecodeError.ReadError(Cause.empty, "Expected a single byte"))

  }
}
