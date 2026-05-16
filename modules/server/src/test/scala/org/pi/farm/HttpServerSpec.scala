package org.pi.farm

import org.pi.farm.model.Message.{Inbound, Outbound}
import org.pi.farm.runtime.*
import org.pi.farm.service.ConfigurationManager
import org.pi.farm.udp.{Queues, QueuesFake, RawMessage}
import org.pi.farm.ws.WSProcessor

import zio.*
import zio.http.{Method, Request, Status, URL}
import zio.internal.stacktracer.SourceLocation
import zio.stream.{Take, ZStream}
import zio.test.*
import zio.test.Assertion.equalTo

import fs2.concurrent.Signal

object HttpServerSpec extends PiFarmSpec {

  private val server = ZLayer {
    for {
      inbound   <- ZIO.service[SignalHub]
      outbound  <- ZIO.service[ResponseQueue]
      scope     <- ZIO.service[Scope]
      processor <- ZIO.service[WSProcessor]
      counter   <- Ref.make(0L)
    } yield new HttpServer(inbound, outbound, scope, processor, counter)
  }

  def spec = suite("HttpServer")(
    suite("Should return 200 OK")(
      testRoute(""),
      testRoute("/"),
      testRoute("index.html")
    )
  ).provideSomeShared[Scope](
    server,
    WSProcessor.live,
    ConfigurationManager.live,
    UIIncomingQueue.live,
    Controllers.live,
    fake.ControllerTypeRepositoryFake.empty,
    fake.ControllerRepositoryFake.empty,
    fake.ConfigurationRepositoryFake.empty,
    fake.PeripheryTypeRepositoryFake.empty,
    fake.ProcessingUnitsRepositoryFake.empty,
    QueuesFake.live,
    SignalStream.live,
    SignalHub.live,
    ResponseQueue.live
  )

  private def testRoute(route: String = "", status: Status = Status.Ok)(using Trace, SourceLocation) =
    test(s"for route '$route'") {
      assertZIO(runRequest(route))(equalTo(status))
    }

  private def runRequest(urlStr: String = "") = {
    val url = if (urlStr.isEmpty) URL.empty else URL.decode(urlStr).toOption.get
    for {
      server <- ZIO.service[HttpServer]
      res    <- server.routes.run(Request(method = Method.GET, url = url))
    } yield res.status
  }
}
