package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class Outbound (controllerId: ControllerId, peripheryId: PeripheryId)

object Outbound {
  given JsonCodec[Outbound] = DeriveJsonCodec.gen[Outbound]
}
