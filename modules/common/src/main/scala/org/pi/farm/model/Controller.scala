package org.pi.farm.model

case class Controller(
  id: ControllerId,         // Unique identifier for the controller
  typeId: ControllerTypeId, // Id of the controller type
  name: String,
  description: String
)

object Controller {
  case class New(typeId: ControllerTypeId, name: String, description: String)
}
