package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class Inbound (controllerId: ControllerId, peripheryId: PeripheryId)

object Inbound {
  given JsonCodec[Inbound] = DeriveJsonCodec.gen[Inbound]
}
