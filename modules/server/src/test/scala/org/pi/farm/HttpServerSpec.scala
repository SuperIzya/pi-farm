package org.pi.farm

import org.pi.farm.model.Message.{Inbound, Outbound}
import org.pi.farm.runtime.*
import org.pi.farm.service.{
  ControllerTypeService,
  FileService,
  FirmwareService,
  FlowConfigurationChanges,
  FlowConfigurationService,
  StorageService
}
import org.pi.farm.storage.AppConfiguration
import org.pi.farm.udp.{Queues, QueuesFake, RawMessage}
import org.pi.farm.ws.WSProcessor

import zio.*
import zio.http.*
import zio.internal.stacktracer.SourceLocation
import zio.stream.{Take, ZStream}
import zio.test.*
import zio.test.Assertion.equalTo

import fs2.concurrent.Signal

object HttpServerSpec extends PiFarmSpec {

  private val server = ZLayer {
    for {
      inbound              <- ZIO.service[SignalHub]
      outbound             <- ZIO.service[ResponseHub]
      processor            <- ZIO.service[WSProcessor]
      serializationService <- ZIO.service[StorageService]
      staticService        <- ZIO.service[FileService]
      counter              <- Ref.make(0L)
    } yield new HttpServer(
      serializationService,
      staticService,
      inbound,
      outbound,
      processor,
      counter,
      "assets/photos"
    )
  }

  def spec = suite("HttpServer")(
    suite("Should return 200 OK")(
      testRoute(""),
      testRoute("/"),
      testRoute("index.html"),
      testRoute("/assets/photos/test.png")
    ),
    suite("POST /api/code")(
      test("returns the staged archive name") {
        for {
          res  <- postCode(
                    FormField.binaryField(
                      "f",
                      Chunk.empty,
                      MediaType.text.plain,
                      filename = Some("a/b.ini")
                    )
                  )
          body <- res.body.asString
        } yield assertTrue(res.status == Status.Ok, body.endsWith(".tar.gz"))
      },
      test("rejects non-file fields") {
        assertZIO(postCode(FormField.simpleField("f", "v")).map(_.status))(
          equalTo(Status.BadRequest)
        )
      }
    )
  ).provideSomeShared[Scope](
    server,
    WSProcessor.live,
    FlowConfigurationService.live,
    UIIncomingQueue.live,
    Controllers.live,
    AppConfiguration.live,
    StorageService.live,
    ControllerTypeService.live,
    fake.StaticServiceFake.live,
    fake.ControllerTypeRepositoryFake.empty,
    fake.ControllerRepositoryFake.empty,
    fake.ConfigurationRepositoryFake.empty,
    fake.PeripheryTypeRepositoryFake.empty,
    fake.ProcessingUnitsRepositoryFake.empty,
    ZLayer.succeed(
      FileService.Config(
        "./data",
        "code",
        "images",
        "/tmp/staging",
        Chunk("platformio.ini", "**/*.cpp"),
        100,
        1024
      )
    ),
    fake.FirmwareServiceFake.empty,
    FlowConfigurationChanges.live,
    QueuesFake.live,
    SignalStream.live,
    SignalHub.live,
    ResponseQueue.live,
    ResponseHub.live
  )

  private def testRoute(route: String = "", status: Status = Status.Ok)(using
    Trace,
    SourceLocation
  ) =
    test(s"for route '$route'") {
      assertZIO(runRequest(route))(equalTo(status))
    }

  private def postCode(field: FormField) = {
    val boundary = Boundary("test-boundary")
    for {
      server <- ZIO.service[HttpServer]
      res    <- server
                  .routes
                  .run(
                    Request
                      .post(URL.root / "api" / "code", Body.fromMultipartForm(Form(field), boundary))
                      .addHeader(Header.ContentType(MediaType.multipart.`form-data`, Some(boundary)))
                  )
    } yield res
  }

  private def runRequest(urlStr: String = "") = {
    val url = if (urlStr.isEmpty) URL.empty else URL.decode(urlStr).toOption.get
    for {
      server <- ZIO.service[HttpServer]
      res    <- server.routes.run(Request(method = Method.GET, url = url))
    } yield res.status
  }
}
