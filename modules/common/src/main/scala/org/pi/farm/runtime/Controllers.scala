package org.pi.farm.runtime

import org.pi.farm.model.{Controller, ControllerId, IpAddress}
import zio.*

import java.net.SocketAddress

class Controllers(byAddress: Ref[Map[IpAddress, Controller]], byId: Ref[Map[ControllerId, IpAddress]]) {
  def addController(address: IpAddress, controller: Controller): UIO[Unit] =
    for {
      _ <- byAddress.update(_ + (address -> controller))
      _ <- byId.update(_ + (controller.id -> address))
    } yield ()

  def getController(address: IpAddress): UIO[Option[Controller]] =
    byAddress.get.map(_.get(address))

  def getAddress(id: ControllerId): UIO[Option[IpAddress]] =
    byId.get.map(_.get(id))

  def listControllerIds: UIO[List[ControllerId]] =
    byId.get.map(_.keys.toList)
}

object Controllers {
  def live: ULayer[Controllers] = ZLayer {
    for {
      byAddress <- Ref.make(Map.empty[IpAddress, Controller])
      byId      <- Ref.make(Map.empty[ControllerId, IpAddress])
    } yield new Controllers(byAddress, byId)
  }
}
