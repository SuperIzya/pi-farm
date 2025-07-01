package org.pi.farm.common

import zio.json.{CamelCase, DeriveJsonCodec, JsonCodec, JsonCodecConfiguration}

sealed trait Message {
  def controllerId: ControllerId
}

object Message {
  sealed trait Inbound extends Message
  sealed trait Outbound extends Message

  case class DataPoint(peripheryId: PeripheryId, value: Double)

  case class Measurement(
                          controllerId: ControllerId, // ID of the controller that sent the measurement
                          dataPoints: List[DataPoint]
  ) extends Inbound

  case class Error(
                        controllerId: ControllerId, // ID of the controller that sent the error
                        message: String // Error message
                      ) extends Inbound

  case class Command(
                      controllerId: ControllerId, // ID of the controller that will receive the command
                      dataPoints: List[DataPoint]
  ) extends Outbound

  case class Discovery(
                        controllerType: ControllerTypeName, // Type of the controller doing the discovered
                        controllerId: ControllerId, // Unique identifier for the controller
                        controllerAddress: IpAddress // IP address of the controller
                      ) extends Inbound

  case class ServerDiscovered(controllerId: ControllerId) extends Outbound

  case class Ping(
                        controllerId: ControllerId // ID of the controller that sent the ping
                      ) extends Inbound
  case class Pong(controllerId: ControllerId) extends Outbound

  given JsonCodec[IpAddress] = JsonCodec[String].transform(
    str => {
      val parts = str.split(":")
      new java.net.InetSocketAddress(parts(0), parts(1).toInt)
    },
    addr => s"${addr.getHostString}:${addr.getPort}"
  )
  given JsonCodec[DataPoint] = DeriveJsonCodec.gen[DataPoint]
  given JsonCodecConfiguration = JsonCodecConfiguration.default.copy(fieldNameMapping = CamelCase)

  given JsonCodec[Inbound] = DeriveJsonCodec.gen[Inbound]
  given JsonCodec[Outbound] = DeriveJsonCodec.gen[Outbound]
  given JsonCodec[Message] = DeriveJsonCodec.gen[Message]
}
