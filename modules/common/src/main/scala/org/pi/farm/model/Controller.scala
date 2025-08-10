package org.pi.farm.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class Controller(
  id: ControllerId,        // Unique identifier for the controller
  typeId: ControllerTypeId // Id of the controller type
)

object Controller {
  given JsonCodec[Controller] = DeriveJsonCodec.gen[Controller]

  case class New(
    typeId: ControllerTypeId 
  )

  object New {
    given JsonCodec[New] = DeriveJsonCodec.gen[New]
  }
}
