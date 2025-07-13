package org.pi.farm.common

case class Controller(
  id: ControllerId,            // Unique identifier for the controller
  typeId: ControllerTypeId,    // Id of the controller type
  peripheries: List[Periphery] // List of peripheries associated with the controller
)
