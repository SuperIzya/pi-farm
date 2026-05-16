package org.pi.farm.runtime

import org.pi.farm.model.{Controller, ControllerId, IpAddress}

import zio.*

import java.net.SocketAddress

class Controllers(byAddress: Ref[Map[String, Controller]], byId: Ref[Map[ControllerId, IpAddress]]) {
  def addController(address: IpAddress, controller: Controller): UIO[Unit] =
    for {
      maybeAddr <- byId.get.map(_.get(controller.id))
      _         <- byAddress.get.map(_ -- maybeAddr.map(_.toString))
      _         <- byAddress.update(_ + (address.toString -> controller))
      _         <- byId.update(_ + (controller.id -> address))
    } yield ()

  def getController(address: IpAddress): UIO[Option[Controller]] =
    byAddress.get.map(_.get(address.toString))

  def getAddress(id: ControllerId): UIO[Option[IpAddress]] =
    byId.get.map(_.get(id))

  def listControllerIds: UIO[List[ControllerId]] =
    byId.get.map(_.keys.toList)
}

object Controllers {
  def live: ULayer[Controllers] = ZLayer {
    for {
      byAddress <- Ref.make(Map.empty[String, Controller])
      byId      <- Ref.make(Map.empty[ControllerId, IpAddress])
    } yield new Controllers(byAddress, byId)
  }
}
