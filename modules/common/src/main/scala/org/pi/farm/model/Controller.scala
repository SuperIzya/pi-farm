package org.pi.farm.model
import zio.json.{DeriveJsonCodec, JsonCodec}

/** A concrete, physical IoT controller deployed in the field — an instance of a [[ControllerType]]. Multiple
  * controllers can share the same type (same board model and wiring layout) while being distinct physical devices.
  *
  * @param id
  *   unique identifier for this controller instance
  * @param typeId
  *   the [[ControllerType]] this controller was built from
  * @param name
  *   human-readable label for this specific device (e.g. "Greenhouse North #2")
  * @param description
  *   additional notes about placement, purpose, or configuration
  */
case class Controller(
  id: ControllerId,
  typeId: ControllerTypeId,
  name: Name,
  description: String
)

object Controller {

  /** Data required to register a new controller (without a system-assigned id). */
  case class New(typeId: ControllerTypeId, name: Name, description: String)

  object New {
    given JsonCodec[New] = DeriveJsonCodec.gen[New]
  }

  given JsonCodec[Controller] = DeriveJsonCodec.gen[Controller]
}
