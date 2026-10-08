package org.pi.farm

import org.pi.farm.model.Types.{*, given}
import org.pi.farm.runtime.*
import org.pi.farm.service.{StaticService, StorageService}
import org.pi.farm.utils.ConfigCompanion
import org.pi.farm.ws.{Command, Data, WSProcessor}

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
  staticService: StaticService,
  inbound: SignalHub,
  outbound: ResponseHub,
  wsProcessor: WSProcessor,
  counter: Ref[Long]
) {

  private final val serializationMap: Map[String, Int => StorageService.EntryStream[Some]] = Map(
    "periphery-type"  -> (x => serializationService.exportPeripheryType(x)),
    "controller-type" -> (x => serializationService.exportControllerType(x)),
    "controller"      -> (x => serializationService.exportController(x)),
    "configuration"   -> (x => serializationService.exportConfiguration(x))
  )

  import HttpServer.binaryCodec

  val routes: Routes[Any, Response] = Routes(
    Chunk.fromIterable(serializationMap.map {
      case (name, exportFunc) =>
        GET / "api" / "export" / name / int("id") -> handler { (id: Int, request: Request) =>
          import StorageService.*

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
    POST / "api" / "import"   -> handler { uploadData },
    GET / "images" / trailing -> handler { (path: Path, request: Request) =>
      staticService.getStaticResource(JPath.of("images").resolve(path.toString)).map {
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
    GET / "appWs"             -> handler(appSocket.toResponse),
    GET / "sensorsWs"         -> handler(sensorsSocket.toResponse),
    GET / trailing            -> Handler.fromFunctionHandler[(Path, Request)] {
      case (path, request) =>
        val fileName = if (path.nonEmpty && path.toString.contains(".")) path else "index.html"
        Handler.fromResource(s"ui/$fileName").contramap(_._2)
    }
  ).sandbox

  private val annotation = zio
    .logging
    .LogAnnotation[Long](
      name = "ws command",
      combine = (_, i) => i,
      render = _.toString
    )

  private def sendFrame(channel: WebSocketChannel)(frame: WebSocketFrame) =
    channel.send(ChannelEvent.read(frame))

  private def sensorsSocket: WebSocketApp[Any] = Handler.webSocket { channel =>
    def send[T: JsonEncoder](stream: ZStream[Any, Nothing, T]) = {
      val s = stream
        .map(_.toJson)
        .flatMap(s => ZStream.unwrap(wsProcessor.splitIfNeeded(s)))
        .mapZIO(sendFrame(channel))

      s.catchAllCause(e => ZStream.unwrap(ZIO.logErrorCause(s"Error sending frame", e).as(s)))
    }

    ZIO.scoped {
      for {
        in  <- inbound.subscribe
        _   <- send(in).runDrain.forkScoped
        out <- outbound.subscribe
        _   <- send(out).runDrain.forkScoped
        _   <- channel.receiveAll(_ => ZIO.unit)
      } yield ()
    }

  }

  private def appSocket: WebSocketApp[Any] = Handler
    .webSocket { channel =>
      ZIO.scoped {
        ZIO.logInfo("WebSocket connected!!!") *>
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
                    _   <- wsProcessor
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
    .tapErrorCauseZIO(ZIO.logErrorCause("Error in websocket", _))

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
    StaticService

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
      staticService        <- ZIO.service[StaticService]
      counter              <- Ref.make(0L)
      pfServer              = new HttpServer(
                                serializationService,
                                staticService,
                                inbound,
                                outbound,
                                wsProcessor,
                                counter
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
