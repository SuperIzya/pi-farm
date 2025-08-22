package org.pi.farm.model

case class Controller(
  id: ControllerId,        // Unique identifier for the controller
  typeId: ControllerTypeId // Id of the controller type
)

object Controller {
  case class New(typeId: ControllerTypeId)
}
