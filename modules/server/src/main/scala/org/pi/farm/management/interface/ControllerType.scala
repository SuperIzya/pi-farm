package org.pi.farm.management.interface

import zio.json.{DeriveJsonEncoder, JsonEncoder}

case class ControllerType(
  id: Int,
  name: String,
  description: String,
  code: String,
  schema: Option[String],
  periphery: List[Int]
)

object ControllerType {
  given JsonEncoder[ControllerType] = DeriveJsonEncoder.gen
}
