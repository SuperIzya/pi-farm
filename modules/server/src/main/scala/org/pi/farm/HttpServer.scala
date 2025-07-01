package org.pi.farm

import org.pi.farm.common.Message.Pong
import org.pi.farm.utils.ConfigCompanion
import zio.{RLayer, Scope, URIO, URLayer, ZIO, ZLayer}
import zio.http.Method.GET
import zio.http.{ChannelEvent, Handler, Request, Response, Routes, Server, WebSocketFrame, handler, int}
import zio.stream.Take
import zio.json.*

class HttpServer(inbound: SignalHub, outbound: ResponseHub, scope: Scope) {

  def routes = Routes(
    GET / "pong" / int("controllerId") -> handler { (controllerId: Int, _: Request) =>
        outbound.offer(Take.single(Pong(controllerId))).as(Response.ok)
    },
    GET / "ws" -> handler(socket.toResponse)
  )

  private def socket = Handler.webSocket{ channel =>
    inbound.toStream.foreach(in => channel.send(ChannelEvent.Read(WebSocketFrame.text(in.toJson))))
      .forkIn(scope) *>
    channel.receiveAll {
      case ChannelEvent.ExceptionCaught(cause) =>
        ZIO.logError(s"WebSocket exception caught: $cause") *>
          channel.shutdown
      case ChannelEvent.Read(message) => ???
      case ChannelEvent.UserEventTriggered(event) => ???
      case ChannelEvent.Registered => ???
      case ChannelEvent.Unregistered => ???
    }
  }
}

object HttpServer {
  type Env = SignalHub & ResponseHub & Scope & Config

  case class Config(port: Int)
  object Config extends ConfigCompanion[Config]("http-server")

  def live: RLayer[Env, Unit] = ZLayer {
    for {
      inbound <- ZIO.service[SignalHub]
      config <- ZIO.service[Config]
      outbound <- ZIO.service[ResponseHub]
      scope <- ZIO.service[Scope]
      server = new HttpServer(inbound, outbound, scope)
      srv <- Server.defaultWithPort(config.port).build(scope)
      _ <- srv.get.install(server.routes).forkScoped
    } yield ()
  }
}
