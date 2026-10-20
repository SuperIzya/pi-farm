package org.pi.farm.runtime

import org.pi.farm.model.Controller
import org.pi.farm.model.Types.{ControllerId, IpAddress}

import zio.*

class Controllers(
  state: Ref[(Map[String, Controller], Map[ControllerId, IpAddress])]
) {
  def addController(address: IpAddress, controller: Controller): UIO[Unit] =
    state.update {
      case (byAddress, byId) =>
        val previousAddress    = byId.get(controller.id)
        val previousController = byAddress.get(address.toString)
        (
          byAddress -- previousAddress.map(_.toString) + (address.toString -> controller),
          byId -- previousController.map(_.id) + (controller.id            -> address)
        )
    }

  def getController(address: IpAddress): UIO[Option[Controller]] =
    state.get.map(_._1.get(address.toString))

  def getAddress(id: ControllerId): UIO[Option[IpAddress]] =
    state.get.map(_._2.get(id))

  def listControllerIds: UIO[List[ControllerId]] =
    state.get.map(_._2.keys.toList)
}

object Controllers {
  def live: ULayer[Controllers] = ZLayer {
    for {
      state <- Ref.make((Map.empty[String, Controller], Map.empty[ControllerId, IpAddress]))
    } yield new Controllers(state)
  }
}
