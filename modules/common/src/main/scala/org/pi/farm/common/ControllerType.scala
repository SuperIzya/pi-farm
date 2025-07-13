package org.pi.farm.common

case class ControllerType(
  id: ControllerTypeId, // Unique identifier for the controller type
  name: String,         // Name of the controller
  description: String,
  code: String,
  periphery: List[PeripheryType]
)
