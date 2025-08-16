package org.pi.farm

import org.pi.farm.model.Message.{Inbound, Outbound}
import org.pi.farm.ws.Processor
import zio.http.{Method, Request, URL}
import zio.stream.Take
import zio.test.{ZIOSpecDefault, assertTrue}
import zio.{Hub, Scope, ZIO, ZLayer}

object HttpServerSpec extends ZIOSpecDefault {

  private val server = ZLayer {
    for {
      inbound <- ZIO.service[SignalHub]
      outbound <- ZIO.service[ResponseHub]
      scope <- ZIO.service[Scope]
      processor <- ZIO.service[Processor]
    } yield new HttpServer(inbound, outbound, scope, processor)
  }

  def spec = suite("HttpServer")(
    suite("Should return 200 OK")(
      test("empty url") {
        for {
          res <- runRequest()
        } yield assertTrue(res.status.code == 200)
      },
      test("/") {
        for {
          res <- runRequest("/")
        } yield assertTrue(res.status.code == 200)
      },
      test("ui/index.html") {
        for {
          res <- runRequest("ui/index.html")
        } yield assertTrue(res.status.code == 200)
      }
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

  private def runRequest(urlStr: String = "") = {
    val url = if(urlStr.isEmpty) URL.empty else URL.decode(urlStr).toOption.get
    for {
      server <- ZIO.service[HttpServer]
      res <- server.routes.run(Request(method = Method.GET, url = url))
    } yield res
  }
}
