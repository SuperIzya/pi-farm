package org.pi.farm

import zio.test.Assertion.equalTo
import org.pi.farm.model.Message.Inbound
import org.pi.farm.model.Message.Outbound
import org.pi.farm.runtime.*
import org.pi.farm.ws.Processor
import zio.*
import zio.http.Method
import zio.http.Request
import zio.http.Status
import zio.http.URL
import zio.internal.stacktracer.SourceLocation
import zio.stream.Take
import zio.test.*

object HttpServerSpec extends ZIOSpecDefault {

  private val server = ZLayer {
    for {
      inbound   <- ZIO.service[SignalHub]
      outbound  <- ZIO.service[ResponseHub]
      scope     <- ZIO.service[Scope]
      processor <- ZIO.service[Processor]
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
    Processor.live,
    fake.ControllerTypeRepositoryFake.empty,
    fake.ControllerRepositoryFake.empty,
    fake.ConfigurationRepositoryFake.empty,
    fake.PeripheryTypeRepositoryFake.empty,
    ZLayer(Hub.sliding[Take[Nothing, Outbound]](16)),
    ZLayer(Hub.sliding[Take[Nothing, Inbound]](16))
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
