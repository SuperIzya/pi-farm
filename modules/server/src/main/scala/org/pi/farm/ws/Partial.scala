package org.pi.farm.ws

import zio.json.{DeriveJsonCodec, JsonCodec}


case class Partial(id: String, data: String, index: Int, totalCount: Int)
object Partial {
  given JsonCodec[Partial] = DeriveJsonCodec.gen
}
