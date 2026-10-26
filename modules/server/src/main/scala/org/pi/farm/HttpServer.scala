package org.pi.farm

import org.pi.farm.model.Message
import org.pi.farm.model.Types.{*, given}
import org.pi.farm.runtime.*
import org.pi.farm.service.{FileService, StorageService}
import org.pi.farm.utils.{Archive, ConfigCompanion}
import org.pi.farm.ws.{Command, Data, ToData, WSProcessor}
import org.pi.farm.ws.ToData.toData

import zio.*
import zio.http.*
import zio.http.Header.HeaderType
import zio.http.Method.{GET, POST}
import zio.http.Server.RequestStreaming
import zio.http.netty.{ChannelType, NettyConfig}
import zio.http.netty.NettyConfig.LeakDetectionLevel
import zio.json.*
import zio.schema.codec.{BinaryCodec, DecodeError}
import zio.stream.{ZPipeline, ZStream}

import java.net.InetSocketAddress
import java.nio.file.{Path => JPath}
import scala.language.implicitConversions

class HttpServer(
  serializationService: StorageService,
  staticService: FileService,
  inbound: SignalHub,
  outbound: ResponseHub,
  wsProcessor: WSProcessor,
  counter: Ref[Long],
  imageDir: String
) {
  import Archive.*
  import StorageService.*
  import HttpServer.binaryCodec

  private final val serializationMap: Map[String, Int => EntryStream[Some]] = Map(
    "periphery-type"  -> (x => serializationService.exportPeripheryType(x)),
    "controller-type" -> (x => serializationService.exportControllerType(x)),
    "controller"      -> (x => serializationService.exportController(x)),
    "configuration"   -> (x => serializationService.exportConfiguration(x))
  )

  val routes: Routes[Any, Response] = Routes(
    Chunk.fromIterable(serializationMap.map {
      case (name, exportFunc) =>
        GET / "api" / "export" / name / int("id") -> handler { (id: Int, request: Request) =>
          val bytes = exportFunc(id).compress
          Response(
            status = Status.Ok,
            headers = Headers(
              Header.ContentDisposition.Attachment(Some(s"$name.tar.gz")),
              Header.ContentType(MediaType.application.`gzip`)
            ),
            body = Body.fromStream(bytes)(using binaryCodec)
          )
        }
    })
  ) ++ Routes(
    POST / "api" / "import"                             -> handler { uploadData },
    POST / "api" / "code"                               -> handler { uploadCode },
    GET / zio.http.codec.PathCodec(imageDir) / trailing -> handler {
      (path: Path, request: Request) =>
        staticService.getStaticResource(JPath.of(imageDir).resolve(path.toString)).map {
          case (length, data) =>
            Response(
              status = Status.Ok,
              headers = Headers(
                Header.ContentLength(length.toLong),
                Header.ContentType(MediaType.image.png)
              ),
              body = Body.fromStream(data)(using binaryCodec)
            )
        }
    },
    GET / "appWs"                                       -> handler(appSocket.toResponse),
    GET / "sensorsWs"                                   -> handler(sensorsSocket.toResponse),
    GET / trailing                                      -> Handler.fromFunctionHandler[(Path, Request)] {
      case (path, request) =>
        Handler
          .fromResource {
            (Path("ui") / (
              if (path.nonEmpty && path.toString.contains(".")) path.dropLeadingSlash.toString
              else "index.html"
            )).toString
          }
          .contramap(_._2)
    }
  ).sandbox

  private val annotation = zio
    .logging
    .LogAnnotation[Long](
      name = "ws command",
      combine = (_, i) => i,
      render = _.toString
    )

  private def sendFrame(channel: WebSocketChannel)(frame: WebSocketFrame): Task[Unit] =
    channel.send(ChannelEvent.read(frame))

  private def sensorsSocket: WebSocketApp[Any] = Handler.webSocket { channel =>
    def send[D <: Data: JsonEncoder](data: D): UIO[Unit] = {
      val s = data.toJson
      ZStream
        .unwrap(wsProcessor.splitIfNeeded(s))
        .foreach(sendFrame(channel))
        .catchAllCause(e => ZIO.logErrorCause(s"Error sending frame", e))
    }

    ZIO.scoped {
      for {
        in  <- inbound.subscribe
        _   <- in.collect {
                 case m: Message.Measurements => m.toData[Data.Measurements]
               }.foreach(send)
                 .forkScoped
        out <- outbound.subscribe
        _   <- out
                 .collect {
                   case m: Message.Command => m.toData[Data.Command]
                 }
                 .foreach(send)
                 .forkScoped
        _   <- channel.receiveAll(_ => ZIO.unit)
      } yield ()
    }

  }

  private def appSocket: WebSocketApp[Any] = Handler
    .webSocket { channel =>
      ZIO.scoped {
        wsProcessor.connect.flatMap { connection =>
          ZIO.logInfo("WebSocket connected!!!") *>
            connection
              .events
              .foreach(sendFrame(channel))
              .catchAllCause(cause =>
                ZIO.logErrorCause("Failed sending firmware events", cause) *> channel.shutdown
              )
              .forkScoped *>
            wsProcessor
              .init
              .flatMap(_.foreach(sendFrame(channel)))
              .forkScoped *>
            channel.receiveAll {
              case ChannelEvent.ExceptionCaught(cause)                     =>
                ZIO.logError(s"WebSocket exception caught: $cause") *> channel.shutdown
              case ChannelEvent.Read(WebSocketFrame.Ping)                  =>
                channel.send(ChannelEvent.read(WebSocketFrame.pong))
              case ChannelEvent.Read(WebSocketFrame.Close(status, reason)) =>
                channel.shutdown *>
                  ZIO.logInfo(s"WebSocket closed with status: $status, reason: $reason")
              case ChannelEvent.Read(WebSocketFrame.Text(message))         =>
                counter.updateAndGet(_ + 1).flatMap { id =>
                  ZIO.logSpan("WS command") {
                    val action = for {
                      _   <- ZIO.logDebug(s"Processing ws command: $message")
                      cmd <- ZIO.fromEither(message.fromJson[Command])
                      _   <- connection
                               .process(cmd)
                               .foreach(sendFrame(channel))
                    } yield ()

                    action.catchAll { e =>
                      val error = s"Failed processing command `$message`: $e"

                      ZIO.logError(error) *>
                        wsProcessor
                          .splitIfNeeded(Data.error(error).toJson)
                          .flatMap(_.foreach(sendFrame(channel)).ignore)
                    } @@ annotation(id)
                  }
                }
              case _                                                       => ZIO.unit
            }
        }
      }
    }
    .tapErrorCauseZIO(ZIO.logErrorCause("Error in websocket", _))

  /** Stages a project folder sent as multipart files named by relative path; returns the archive
    * name.
    */
  private def uploadCode(req: Request): IO[Response, Response] =
    ZIO
      .fail(new IllegalArgumentException("Expected multipart/form-data"))
      .unless(req.header(Header.ContentType).exists(_.mediaType == MediaType.multipart.`form-data`))
      .zipRight(req.body.asMultipartFormStream)
      .flatMap { form =>
        staticService.stageCode(form.fields.mapZIO { field =>
          ZIO
            .fromOption(field.filename)
            .orElseFail(new IllegalArgumentException(s"Form field '${field.name}' is not a file"))
            .map(_ -> field.asStream)
        })
      }
      .map(Response.text)
      .tapErrorCause(ZIO.logErrorCause("Failed to stage code upload", _))
      .mapError {
        case e: IllegalArgumentException => Response.badRequest(e.getMessage)
        case e                           => Response.internalServerError(s"Failed to stage code: ${e.getMessage}")
      }

  private def uploadData(req: Request): IO[Response, Response] = {
    if (req.header(Header.ContentType).exists(_.mediaType == MediaType.multipart.`form-data`))
      for {
        _    <- ZIO.logDebug("Starting to read multipart/form stream")
        form <- req
                  .body
                  .asMultipartFormStream
                  .mapError(ex =>
                    Response(
                      Status.InternalServerError,
                      body = Body.fromString(
                        s"Failed to decode body as multipart/form-data (${ex.getMessage}"
                      )
                    )
                  )

        _ <- form
               .fields
               .tap(f => ZIO.log(s"started reading new field: ${f.name}"))
               .mapZIO {
                 case sb: FormField.StreamingBinary =>
                   serializationService
                     .importData(sb.data)
                     .tapError(err => ZIO.logError(s"Failed to import data: $err"))
                 case _                             =>
                   ZIO.unit
               }
               .runDrain
               .mapError(ex =>
                 Response(
                   Status.InternalServerError,
                   body = Body
                     .fromString(s"Failed to process multipart/form-data field (${ex.getMessage})")
                 )
               )

        _ <- ZIO.logDebug(s"Finished reading multipart/form stream")
      } yield Response.text("OK")
    else ZIO.succeed(Response(status = Status.NotFound))
  }

}

object HttpServer {
  type Env = Config & SignalHub & ResponseHub & WSProcessor & UIIncomingQueue & StorageService &
    FileService & FileService.Config

  private def serverConfig(config: Config): Server.Config =
    Server
      .Config
      .default
      .copy(
        address = new InetSocketAddress(config.address, config.port),
        requestStreaming = RequestStreaming.Enabled
      )

  private def nettyConfig(config: Config): NettyConfig =
    NettyConfig
      .default
      .copy(
        nThreads = config.numThreads,
        leakDetectionLevel = LeakDetectionLevel.ADVANCED,
        channelType = ChannelType.NIO,
        bossGroup = NettyConfig
          .default
          .bossGroup
          .copy(
            nThreads = config.numThreads,
            channelType = ChannelType.NIO
          )
      )

  private def driver: RLayer[Config, Server] =
    (
      ZLayer
        .fromFunction(serverConfig) ++
        ZLayer.fromFunction(nettyConfig)
    ) >>> Server.customized

  def live: RLayer[Env, Unit] = driver >>> ZLayer.scoped {
    for {
      inbound              <- ZIO.service[SignalHub]
      outbound             <- ZIO.service[ResponseHub]
      wsProcessor          <- ZIO.service[WSProcessor]
      serializationService <- ZIO.service[StorageService]
      staticService        <- ZIO.service[FileService]
      staticConfig         <- ZIO.service[FileService.Config]
      counter              <- Ref.make(0L)
      pfServer              = new HttpServer(
                                serializationService,
                                staticService,
                                inbound,
                                outbound,
                                wsProcessor,
                                counter,
                                staticConfig.imageDir
                              )
      _                    <- pfServer.routes.serve.forkScoped
      scope                <- ZIO.scope
      _                    <- scope.addFinalizer(ZIO.logInfo("Shutting down HTTP server"))
    } yield ()
  }

  case class Config(port: Int, address: String, numThreads: Int)

  object Config extends ConfigCompanion[Config]("http-server")

  private final val binaryCodec: BinaryCodec[Byte] = new BinaryCodec[Byte] {

    val streamDecoder: ZPipeline[Any, DecodeError, Byte, Byte] = ZPipeline.identity

    val streamEncoder: ZPipeline[Any, Nothing, Byte, Byte] = ZPipeline.identity

    def encode(a: Byte): Chunk[Byte] = Chunk.single(a)

    def decode(chunk: Chunk[Byte]): Either[DecodeError, Byte] =
      Either.cond(
        chunk.size == 1,
        chunk(0),
        DecodeError.ReadError(Cause.empty, "Expected a single byte")
      )

  }
}
