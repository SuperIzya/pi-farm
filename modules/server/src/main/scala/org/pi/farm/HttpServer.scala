package org.pi.farm

import org.pi.farm.utils.ConfigCompanion
import org.pi.farm.ws.{Command, Data, WSProcessor}
import org.pi.farm.runtime.*
import zio.*
import zio.http.*
import zio.http.Method.GET
import zio.json.*

class HttpServer(
  inbound: SignalHub,
  outbound: ResponseHub,
  scope: Scope,
  wsProcessor: WSProcessor,
  counter: Ref[Long]
) {

  val routes: Routes[Any, Response] = Routes(
    GET / "ws"     -> handler(socket.toResponse),
    GET / trailing -> Handler.fromFunctionHandler[(Path, Request)] {
      case (path, request) =>
        val fileName = if (path.nonEmpty && path.toString.contains(".")) path else "index.html"
        Handler.fromResource(s"ui/$fileName").contramap(_._2)
    }
  ).sandbox
  private val annotation = zio.logging.LogAnnotation[Long](
    name = "ws command",
    combine = (_, i) => i,
    render = _.toString
  )
  private def socket: WebSocketApp[Any] = Handler
    .webSocket { channel =>
      def sendFrame(frame: WebSocketFrame): Task[Unit] =
        channel.send(ChannelEvent.read(frame))

      ZIO.logInfo("WebSocket connected") *>
        inbound.toStream.foreach(in => sendFrame(WebSocketFrame.text(in.toJson))).forkIn(scope) *>
        channel.receiveAll {
          case ChannelEvent.ExceptionCaught(cause) =>
            ZIO.logError(s"WebSocket exception caught: $cause") *> channel.shutdown

          case ChannelEvent.Read(WebSocketFrame.Text(message)) =>
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
          case ChannelEvent.Read(WebSocketFrame.Ping) =>
            channel.send(ChannelEvent.read(WebSocketFrame.pong))
          case ChannelEvent.Read(WebSocketFrame.Close(status, reason)) =>
            channel.shutdown *>
              ZIO.logInfo(s"WebSocket closed with status: $status, reason: $reason")
          case _ => ZIO.unit
        }
    }
    .tapErrorCauseZIO(ZIO.logErrorCause("Error in websocket", _))

}

object HttpServer {
  type Env = SignalHub & ResponseHub & Scope & WSProcessor & Server & UIIncomingQueue

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound     <- ZIO.service[SignalHub]
      outbound    <- ZIO.service[ResponseHub]
      scope       <- ZIO.service[Scope]
      wsProcessor <- ZIO.service[WSProcessor]
      counter     <- Ref.make(0L)
      server = new HttpServer(inbound, outbound, scope, wsProcessor, counter)
      _ <- server.routes.serve.forkScoped
      _ <- ZIO.logInfo(s"HTTP server started")
    } yield ()
  }

  case class Config(port: Int)

  object Config extends ConfigCompanion[Config]("http-server")
}
