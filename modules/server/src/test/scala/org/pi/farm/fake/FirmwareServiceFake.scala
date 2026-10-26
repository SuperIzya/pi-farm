package org.pi.farm.fake
import org.pi.farm.model.FirmwareRequest.Action
import org.pi.farm.model.Process.{Output, Phase, Status}
import org.pi.farm.model.Types.ControllerId
import org.pi.farm.service.FirmwareService

import zio.{Scope, Task, UIO, ULayer, ZIO, ZLayer}
import zio.stream.ZStream

import java.nio.file.Path

object FirmwareServiceFake {
  val empty: ULayer[FirmwareService] = ZLayer.succeed(new Empty())

  final class Empty() extends FirmwareService {
    private val idle = Status(Phase.Idle, None, None, None)

    def start(
      controllerId: ControllerId,
      action: Action,
      port: Option[String] = None
    ): Task[Unit] = ZIO.unit

    def await: UIO[Status] = ZIO.succeed(idle)

    def cancel: UIO[Unit] = ZIO.unit

    def status: UIO[Status] = ZIO.succeed(idle)

    def statuses: ZStream[Any, Nothing, Status] = ZStream.empty

    def output: ZIO[Scope, Nothing, ZStream[Any, Nothing, Output]] =
      ZIO.succeed(ZStream.empty)

  }
}
