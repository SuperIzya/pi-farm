package org.pi.farm

import org.pi.farm.utils.ConfigCompanion
import org.pi.farm.ws.{Command, Processor}
import zio.http.*
import zio.http.Method.GET
import zio.json.*
import zio.{RLayer, Scope, ZIO, ZLayer}

class HttpServer(inbound: SignalHub, outbound: ResponseHub, scope: Scope, processor: Processor) {

  def routes: Routes[Any, Throwable] = Routes(
    GET / Root     -> Handler.getResourceAsFile("ui/index.html").flatMap(Handler.fromFile(_)),
    GET / "ws"     -> handler(socket.toResponse),
    GET / "ui" / trailing -> handler {
      val extractPath    = Handler.param[(Path, Request)](_._1)
      val extractRequest = Handler.param[(Path, Request)](_._2)

      for {
        path <- extractPath.map(_.encode)
        fileName = if (path.contains(".")) path else "index.html"
        file <- Handler.getResourceAsFile(s"ui/$fileName")
        http <- extractRequest >>> (if (file.isFile && file.exists) Handler.fromFile(file) else Handler.notFound)
      } yield http
    }
  )

  private def socket = Handler.webSocket { channel =>
    inbound.toStream
      .foreach(in => channel.send(ChannelEvent.Read(WebSocketFrame.text(in.toJson))))
      .forkIn(scope) *>
      channel.receiveAll {
        case ChannelEvent.ExceptionCaught(cause) =>
          ZIO.logError(s"WebSocket exception caught: $cause") *>
            channel.shutdown
        case ChannelEvent.Read(WebSocketFrame.Text(message)) =>
          val action = for {
            cmd      <- ZIO.fromEither(message.fromJson[Command])
            response <- processor.process(cmd)
            _        <- channel.send(ChannelEvent.Read(response))
          } yield ()

          action.catchAll { e =>
            ZIO.logError(s"Error processing command: $e")
          }
        case ChannelEvent.Read(WebSocketFrame.Ping) =>
          channel.send(ChannelEvent.Read(WebSocketFrame.Pong))
        case ChannelEvent.Read(WebSocketFrame.Close(status, reason)) =>
          channel.shutdown *>
            ZIO.logInfo(s"WebSocket closed with status: $status, reason: $reason")
        case _ => ZIO.unit
      }
  }

  private def serveFile(file: String): Handler[Any, Throwable, Nothing, Response] =
    Handler.getResourceAsFile(s"ui/$file").flatMap { f =>
      if (f.exists) {
        Handler.fromFile(f)
      } else {
        Handler.notFound
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
      _ <- server.routes.sandbox.serve.forkScoped
      _ <- ZIO.logInfo(s"HTTP server started")
    } yield ()
  }

  case class Config(port: Int)

  object Config extends ConfigCompanion[Config]("http-server")
}
