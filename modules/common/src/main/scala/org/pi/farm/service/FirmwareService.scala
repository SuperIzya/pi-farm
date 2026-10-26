package org.pi.farm.service

import org.pi.farm.model.FirmwareRequest.Action
import org.pi.farm.model.Process.{Output, Phase, Status}
import org.pi.farm.model.Types.*
import org.pi.farm.model.UdpConfig
import org.pi.farm.storage.ControllerRepository
import org.pi.farm.utils.ConfigCompanion

import zio.*
import zio.json.*
import zio.process.{Command, CommandError, Process}
import zio.stream.{SubscriptionRef, ZStream}

import java.nio.file.{Files, Path}
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit
import scala.concurrent.TimeoutException
import scala.jdk.CollectionConverters.*

trait FirmwareService {
  def start(
    controllerId: ControllerId,
    action: Action,
    port: Option[String] = None
  ): Task[Unit]
  def await: UIO[Status]
  def cancel: UIO[Unit]
  def status: UIO[Status]
  def statuses: ZStream[Any, Nothing, Status]
  def output: ZIO[Scope, Nothing, ZStream[Any, Nothing, Output]]
}

object FirmwareService {

  case class Config(wifiSsid: String, wifiPassword: String, timeout: Duration)
  object Config extends ConfigCompanion[Config]("firmware")

  case object Busy extends Exception("A firmware operation is already running")

  private case class Operation(
    done: Promise[Nothing, Status],
    fiber: Promise[Nothing, Fiber.Runtime[Nothing, Unit]]
  )
  private case class State(status: Status, operation: Option[Operation], running: Boolean)
  private case class Device(port: String, description: String, hwid: String)
  private object Device {
    given JsonDecoder[Device] = DeriveJsonDecoder.gen[Device]
  }

  val live: RLayer[
    Config & FileService.Config & FileService & ControllerRepository & UdpConfig,
    FirmwareService
  ] = ZLayer.scoped {
    for {
      config      <- ZIO.service[Config]
      files       <- ZIO.service[FileService.Config]
      storage     <- ZIO.service[FileService]
      controllers <- ZIO.service[ControllerRepository]
      udp         <- ZIO.service[UdpConfig]
      manager     <- make(
                       config,
                       files,
                       storage,
                       id =>
                         controllers
                           .get(id)
                           .someOrFail(new IllegalArgumentException(s"Unknown controller: $id"))
                           .map(_.typeId),
                       udp.port
                     )
    } yield manager
  }

  private def touch(path: Path): Task[Unit] =
    for {
      now <- Clock.instant
      _   <- ZIO.attemptBlocking {
               Files.setLastModifiedTime(path, FileTime.from(now))
             }
    } yield ()

  private[service] type Runner =
    (Command, Path, Map[String, String], Output => UIO[Unit]) => RIO[Scope, Execution]

  private[service] def make(
    config: Config,
    files: FileService.Config,
    storage: FileService,
    controllerType: ControllerId => Task[ControllerTypeId],
    udpPort: Int,
    run: Runner = FirmwareService.run
  ): ZIO[Scope, Throwable, FirmwareService] =
    for {
      _     <- ZIO
                 .fail(new IllegalArgumentException("Firmware timeout must be positive"))
                 .when(config.timeout <= Duration.Zero)
      scope <- ZIO.service[Scope]
      state <- SubscriptionRef.make(State(Status(Phase.Idle), None, false))
      hub   <- ZIO.acquireRelease(Hub.sliding[Output](2))(_.shutdown)

      constEnvironment = Map(
                           "WIFI_SSID"     -> config.wifiSsid,
                           "WIFI_PASSWORD" -> config.wifiPassword,
                           "UDP_PORT"      -> udpPort.toString
                         )

    } yield new Live(
      state,
      hub,
      storage,
      controllerType,
      constEnvironment,
      config.timeout,
      scope,
      run
    )

  private final class Live(
    state: SubscriptionRef[State],
    hub: Hub[Output],
    storage: FileService,
    controllerType: ControllerId => Task[ControllerTypeId],
    constEnvironment: Map[String, String],
    timeout: Duration,
    scope: Scope,
    run: Runner
  ) extends FirmwareService {
    val status   = state.get.map(_.status)
    val statuses = state.changes.map(_.status).changes
    val await    = state.get.flatMap(_.operation.fold(status)(_.done.await))
    val cancel   = state.get.flatMap { s =>
      ZIO.foreachDiscard(s.operation.filter(_ => s.running))(op =>
        op.fiber
          .await
          .flatMap(fiber =>
            fiber.interrupt *> fiber.await *> op.done.await *> state.update(
              _.copy(running = false)
            )
          )
      )
    }
    val output   = hub.subscribe.map(queue => ZStream.fromQueue(queue))

    private def emit(line: Output)(using Trace): UIO[Unit] = (line match {
      case Output.Stderr(message) => ZIO.logWarning(message)
      case Output.Stdout(message) => ZIO.logInfo(message)
    }) *>
      hub.publish(line).unit

    private def prepareWorkspace(id: ControllerId): RIO[Scope, Path] =
      storage.withPublicationLock {
        for {
          typeId <- controllerType(id)
          path   <- storage.unpackCode(typeId)
          _      <- storage
                      .withPublicationLock(touch(path))
                      .catchAllCause(
                        ZIO.logErrorCause("Failed to touch firmware workspace", _)
                      )
                      .repeat(Schedule.windowed(1.hour))
                      .forkScoped
        } yield path
      }

    private def environmentWithControllerId(id: ControllerId): Map[String, String] =
      constEnvironment + ("CONTROLLER_ID" -> id.toString)

    private def getUSBPort(workspace: Path) =
      for {
        lines      <- Ref.make(List.empty[String])
        _          <- emit(Output.stdout("Running `pio device list --json-output`"))
        executable <- run(
                        Command("pio", "device", "list", "--json-output"),
                        workspace,
                        Map.empty,
                        line =>
                          emit(line) *>
                            lines.update(xs =>
                              line match {
                                case Output.Stdout(text) => text :: xs
                                case _                   => xs
                              }
                            )
                      )
        _          <- executable.waitFor
        text       <- lines.get.map(_.reverse.mkString("\n"))
        ports      <-
          ZIO
            .fromEither(text.fromJson[List[Device]])
            .mapError(e => new Exception(s"Invalid USB detection output: $e"))
            .map(_.filterNot { d =>
              d.description.isEmpty ||
              d.description == "n/a" ||
              d.hwid.isEmpty ||
              d.hwid == "n/a" ||
              d.port.isEmpty ||
              d.port == "n/a"
            })
        _          <- ZIO
                        .fail(
                          new IllegalArgumentException(
                            "Expected exactly one USB device; select an explicit upload port"
                          )
                        )
                        .tapError(err => emit(Output.stderr(err.toString)))
                        .unless(ports.size == 1)
      } yield Some(ports.head.port)

    private def execute(id: ControllerId, action: Action, port: Option[String]): Task[Status] =
      ZIO.scoped {
        val task = for {
          workspace  <- prepareWorkspace(id)
          environment = environmentWithControllerId(id)
          selected   <- getUSBPort(workspace)
                          .when(action == Action.Upload && port.isEmpty)
                          .someOrElse(port)
          _          <- state.update(s =>
                          s.copy(status =
                            Status(
                              if (action == Action.Build) Phase.Building
                              else Phase.Uploading,
                              Some(id)
                            )
                          )
                        )
          arguments   = (if (action == Action.Upload)
                           Seq("--target", "upload", "--upload-port", selected.get)
                         else Seq.empty)
          command     = Command("pio", ("run" +: arguments)*)
          _          <- emit(Output.stdout(s"Running command: ${command}"))
          _          <- run(command, workspace, environment, emit).flatMap(_.waitFor)
        } yield ()

        task.exit.map {
          case Exit.Success(_) =>
            Status(
              phase = if (action == Action.Build) Phase.Succeeded else Phase.Succeeded,
              controllerId = Some(id),
              exitCode = Some(0),
              error = None
            )
          case Exit.Failure(e) =>
            Status(
              phase = if (action == Action.Build) Phase.Failed else Phase.Failed,
              controllerId = Some(id),
              exitCode = Some(1),
              error = Some(e.squash.getMessage())
            )
        }
      }

    def start(id: ControllerId, action: Action, port: Option[String]): Task[Unit] =
      ZIO.uninterruptibleMask { restore =>
        for {
          _        <- ZIO
                        .fail(new IllegalArgumentException("Upload port must not be empty"))
                        .when(port.exists(_.trim.isEmpty))
          done     <- Promise.make[Nothing, Status]
          fiber    <- Promise.make[Nothing, Fiber.Runtime[Nothing, Unit]]
          op        = Operation(done, fiber)
          accepted <- state.modify { s =>
                        if (s.running) (false, s)
                        else (true, State(Status(Phase.Preparing, Some(id)), Some(op), true))
                      }
          _        <- ZIO.fail(Busy).unless(accepted)
          work      = restore(
                        execute(id, action, port)
                          .timeout(timeout)
                          .map {
                            case Some(value) => value
                            case None        => Status(Phase.Timeout, Some(id))
                          }
                      ).exit.flatMap { exit =>
                        val result = exit match {
                          case Exit.Success(value) => value
                          case Exit.Failure(cause) =>
                            Status(
                              phase = if (cause.isInterruptedOnly) Phase.Canceled else Phase.Failed,
                              controllerId = Some(id),
                              exitCode = Some(1),
                              error = Some(cause.squash.getMessage)
                            )
                        }
                        ZIO.foreachDiscard(exit.causeOption.filterNot(_.isInterruptedOnly))(
                          ZIO.logErrorCause("Firmware operation failed", _)
                        ) *>
                          state.update(s =>
                            s.copy(status = result, running = result.phase == Phase.Canceled)
                          ) *>
                          done.succeed(result).unit
                      }
          running  <- work.forkIn(scope)
          _        <- fiber.succeed(running)
        } yield ()
      }
  }

  final case class Execution(
    process: Option[Process],
    readers: Chunk[Fiber.Runtime[CommandError, Unit]]
  ) {
    val interrupt: UIO[Execution] =
      process.fold(ZIO.unit)(FirmwareService.stop) *>
        ZIO.foreachDiscard(readers)(_.interrupt).as(Execution.empty)

    val waitFor: Task[ExitCode] = process.fold(ZIO.succeed(ExitCode(0)))(process =>
      process.exitCode.flatMap(code => ZIO.foreachDiscard(readers)(_.join).as(code))
    )
  }

  object Execution {
    val empty: Execution = Execution(None, Chunk.empty)
  }

  def run(
    command: Command,
    workspace: Path,
    environment: Map[String, String],
    emit: Output => UIO[Unit]
  ): RIO[Scope, Execution] = {
    for {
      process <- command
                   .env(environment)
                   .workingDirectory(workspace.toFile)
                   .run
      stderr  <- process.stderr.linesStream.foreach(line => emit(Output.stderr(line))).forkScoped
      stdout  <- process.stdout.linesStream.foreach(line => emit(Output.stdout(line))).forkScoped
      _       <- ZIO.addFinalizer(stop(process))
    } yield Execution(Some(process), Chunk(stderr, stdout))
  }

  private def stop(process: Process): UIO[Unit] =
    process
      .execute { child =>
        val descendants = child.toHandle.descendants()
        val children    = try descendants.iterator().asScala.toList.reverse
        finally descendants.close()
        children.foreach(_.destroyForcibly())
        if (!child.waitFor(1, TimeUnit.SECONDS)) child.destroyForcibly()
        child.waitFor()
        child.getInputStream.close()
        child.getErrorStream.close()
        child.getOutputStream.close()
      }
      .tapErrorCause(ZIO.logErrorCause("Failed to stop firmware process", _))
      .orDie
      .unit

}
