package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}
import scala.language.implicitConversions

case class Address(controllerId: ControllerId, peripheryId: PeripheryId, name: Name)

object Address {
  given JsonCodec[Address] = DeriveJsonCodec.gen[Address]
}
