package org.pi.farm

import org.pi.farm.utils.ConfigCompanion
import org.pi.farm.ws.{Command, Processor}
import zio.{RLayer, Scope, ZIO, ZLayer}
import zio.http.*
import zio.http.Method.GET
import zio.json.*

class HttpServer(inbound: SignalHub, outbound: ResponseHub, scope: Scope, processor: Processor) {

  val routes: Routes[Any, Response] = Routes(
    GET / "ws"     -> handler(socket.toResponse),
    GET / trailing -> handler {
      val extractPath    = Handler.param[(Path, Request)](_._1)
      val extractRequest = Handler.param[(Path, Request)](_._2)

      for {
        path <- extractPath.map(_.encode).map { p => if (p.startsWith("/")) p.drop(1) else p }
        fileName = if (path.nonEmpty && path.contains(".")) path else "index.html"
        file <- Handler.fromResource(s"ui/$fileName")
      } yield file
    }
  ).sandbox

  private def socket: WebSocketApp[Any] = Handler.webSocket { channel =>
    ZIO.logInfo("WebSocket connected") *>
      inbound.toStream.foreach(in => channel.send(ChannelEvent.Read(WebSocketFrame.text(in.toJson)))).forkIn(scope) *>
      channel.receiveAll {
        case ChannelEvent.ExceptionCaught(cause)                     =>
          ZIO.logError(s"WebSocket exception caught: $cause") *>
            channel.shutdown
        case ChannelEvent.Read(WebSocketFrame.Text(message))         =>
          val action = for {
            cmd      <- ZIO.fromEither(message.fromJson[Command])
            response <- processor.process(cmd)
            _        <- response.fold(ZIO.unit)(data => channel.send(ChannelEvent.read(data)))
          } yield ()

          action.catchAll { e => ZIO.logError(s"Error processing command: $e") }
        case ChannelEvent.Read(WebSocketFrame.Ping)                  =>
          channel.send(ChannelEvent.read(WebSocketFrame.pong))
        case ChannelEvent.Read(WebSocketFrame.Close(status, reason)) =>
          channel.shutdown *>
            ZIO.logInfo(s"WebSocket closed with status: $status, reason: $reason")
        case _                                                       => ZIO.unit
      }
  }
}

object HttpServer {
  type Env = SignalHub & ResponseHub & Scope & Processor & Server

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound   <- ZIO.service[SignalHub]
      outbound  <- ZIO.service[ResponseHub]
      scope     <- ZIO.service[Scope]
      processor <- ZIO.service[Processor]
      server = new HttpServer(inbound, outbound, scope, processor)
      _ <- server.routes.serve.forkScoped
      _ <- ZIO.logInfo(s"HTTP server started")
    } yield ()
  }

  case class Config(port: Int)

  object Config extends ConfigCompanion[Config]("http-server")
}
