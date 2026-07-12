package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

import scala.language.implicitConversions

import Types.*

case class Address(
  controllerId: ControllerId,
  peripheryName: PeripheryName,
  peripjeryChannel: PeripheryChannelName,
  processorConnectionName: Name
)

object Address {
  given JsonCodec[Address] = DeriveJsonCodec.gen[Address]
}
