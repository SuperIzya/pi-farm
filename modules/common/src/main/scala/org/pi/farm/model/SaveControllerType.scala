package org.pi.farm.model

case class SaveControllerType(
  id: Option[ControllerTypeId], // Unique identifier for the controller type
  name: String,         // Name of the controller
  description: String,
  code: String,
  periphery: Map[String, PeripheryType]
)

object SaveControllerType {
  import zio.json.{DeriveJsonDecoder, JsonDecoder}

  // Automatically derive a JSON decoder for SaveControllerType
  given JsonDecoder[SaveControllerType] = DeriveJsonDecoder.gen[SaveControllerType]
}
