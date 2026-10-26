package org.pi.farm.model

import org.pi.farm.model.FirmwareRequest.Action
import org.pi.farm.model.Types.ControllerId

import zio.json.*

final case class FirmwareRequest(
  controllerId: ControllerId,
  action: Action,
  port: Option[String]
)
object FirmwareRequest {
  enum Action {
    case Build, Upload
  }

  given JsonCodec[Action] = JsonCodec[String].transformOrFail(
    {
      case "build"  => Right(Action.Build)
      case "upload" => Right(Action.Upload)
      case other    => Left(s"Invalid action: $other")
    },
    {
      case Action.Build  => "build"
      case Action.Upload => "upload"
    }
  )

  given JsonCodec[FirmwareRequest] = DeriveJsonCodec.gen[FirmwareRequest]
}
