package org.pi.farm.plugin.syntax

import org.pi.farm.model.Message
import org.pi.farm.model.Types.*
import org.pi.farm.plugin.*

import zio.Chunk

trait OutletsSetter[Out <: NonEmptyTuple] {
  def convertToData(
    out: Out,
    outlets: TOutlets[Out],
    outletsMap: Map[Outlet[?], (ControllerId, PeripheryName, PeripheryChannelName)]
  ): Chunk[Message.FlatDataPacket]
}

object OutletsSetter {

  def apply[Out <: NonEmptyTuple](using setter: OutletsSetter[Out]): OutletsSetter[Out] = setter

  given scalar[Out: NotTuple]: OutletsSetter[Out *: EmptyTuple] with {
    def convertToData(
      out: Out *: EmptyTuple,
      outlets: TOutlets[Out *: EmptyTuple],
      outletsMap: Map[Outlet[?], (ControllerId, PeripheryName, PeripheryChannelName)]
    ): Chunk[Message.FlatDataPacket] = {
      val outlet: Outlet[Out] = outlets.head
      val value: Out          = out.head
      val address             = outletsMap(outlet)
      Chunk(Message.FlatDataPacket(address._1, address._2, address._3, outlet.format(value)))
    }
  }

  given step[H: NotTuple, T <: NonEmptyTuple](using tailSetter: OutletsSetter[T]): OutletsSetter[H *: T] with {
    def convertToData(
      out: H *: T,
      outlets: TOutlets[H *: T],
      outletsMap: Map[Outlet[?], (ControllerId, PeripheryName, PeripheryChannelName)]
    ): Chunk[Message.FlatDataPacket] = {
      val value   = out.head
      val outlet  = outlets.head
      val address = outletsMap(outlet)
      Chunk(Message.FlatDataPacket(address._1, address._2, address._3, outlet.format(value))) ++
        tailSetter.convertToData(out.tail, outlets.tail, outletsMap)
    }
  }
}
