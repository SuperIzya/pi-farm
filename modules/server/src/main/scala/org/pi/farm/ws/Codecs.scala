package org.pi.farm.ws

import org.pi.farm.model
import zio.json.{DeriveJsonCodec, JsonCodec}

object Codecs {
  given JsonCodec[model.Inbound] = DeriveJsonCodec.gen[model.Inbound]
  given JsonCodec[model.ControllerType] = DeriveJsonCodec.gen[model.ControllerType]
  given JsonCodec[model.PeripheryType] = DeriveJsonCodec.gen[model.PeripheryType]
  given JsonCodec[model.Controller] = DeriveJsonCodec.gen[model.Controller]
  given JsonCodec[model.Configuration] = DeriveJsonCodec.gen[model.Configuration]
  given JsonCodec[model.Outbound] = DeriveJsonCodec.gen[model.Outbound]
  given peripheryNew: JsonCodec[model.PeripheryType.New] = DeriveJsonCodec.gen[model.PeripheryType.New]
  given controllerTypeNew: JsonCodec[model.ControllerType.New] = DeriveJsonCodec.gen[model.ControllerType.New]
  given controllerNew: JsonCodec[model.Controller.New] = DeriveJsonCodec.gen[model.Controller.New]

}
