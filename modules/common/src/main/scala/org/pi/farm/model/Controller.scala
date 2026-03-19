package org.pi.farm.model
import zio.json.{DeriveJsonCodec, JsonCodec}

case class Controller(
  id: ControllerId,         // Unique identifier for the controller
  typeId: ControllerTypeId, // Id of the controller type
  name: Name,
  description: String
)

object Controller {
  case class New(typeId: ControllerTypeId, name: Name, description: String)

  object New {
    given JsonCodec[New] = DeriveJsonCodec.gen[New]
  }

  given JsonCodec[Controller] = DeriveJsonCodec.gen[Controller]
}
