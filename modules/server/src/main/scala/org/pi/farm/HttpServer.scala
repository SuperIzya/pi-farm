package org.pi.farm

import org.pi.farm.common.Message.Pong
import org.pi.farm.utils.ConfigCompanion
import zio.{RLayer, Scope, URIO, URLayer, ZIO, ZLayer}
import zio.http.Method.GET
import zio.http.*
import zio.stream.Take
import zio.json.*

class HttpServer(inbound: SignalHub, outbound: ResponseHub, scope: Scope) {

  def routes: Routes[Any, Throwable] = Routes(
    GET / "ws"     -> handler(socket.toResponse),
    GET / Root     -> handler(Handler.getResourceAsFile("ui/index.html").flatMap(Handler.fromFile(_))),
    GET / trailing -> handler {
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

  private def serveFile(file: String): Handler[Any, Throwable, Nothing, Response] =
    Handler.getResourceAsFile(s"ui/$file").flatMap { f =>
      if (f.exists) {
        Handler.fromFile(f)
      } else {
        Handler.notFound
      }
    }

  private def socket = Handler.webSocket { channel =>
    inbound.toStream
      .foreach(in => channel.send(ChannelEvent.Read(WebSocketFrame.text(in.toJson))))
      .forkIn(scope) *>
      channel.receiveAll {
        case ChannelEvent.ExceptionCaught(cause) =>
          ZIO.logError(s"WebSocket exception caught: $cause") *>
            channel.shutdown
        case ChannelEvent.Read(WebSocketFrame.Text(message)) => ???
        case ChannelEvent.Read(WebSocketFrame.Ping)          =>
          channel.send(ChannelEvent.Read(WebSocketFrame.Pong))
        case ChannelEvent.Read(WebSocketFrame.Close(status, reason)) =>
          channel.shutdown *>
            ZIO.logInfo(s"WebSocket closed with status: $status, reason: $reason")
        case _ => ZIO.unit
      }
  }
}

object HttpServer {
  type Env = SignalHub & ResponseHub & Scope & Config

  case class Config(port: Int)
  object Config extends ConfigCompanion[Config]("http-server")

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound  <- ZIO.service[SignalHub]
      config   <- ZIO.service[Config]
      outbound <- ZIO.service[ResponseHub]
      scope    <- ZIO.service[Scope]
      server = new HttpServer(inbound, outbound, scope)
      srv <- Server.defaultWithPort(config.port).build(scope)
      _   <- srv.get.install(server.routes.sandbox).forkScoped
    } yield ()
  }
}
