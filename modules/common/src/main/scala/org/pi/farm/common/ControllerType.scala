package org.pi.farm.common

case class ControllerType(
  name:        String,    // Name of the controller
  description: String,
  code:        String,
  periphery:   List[PeripheryType]
)
