package org.pi.farm.plugin.syntax

import org.pi.farm.model.*
import org.pi.farm.plugin.*
import zio.Chunk

trait OutletsSetter[Out <: NonEmptyTuple] {
  def convertToData(
    out: Out,
    outlets: TOutlets[Out],
    outletsMap: Map[Outlet[?], (ControllerId, PeripheryId)]
  ): Chunk[Message.DataPacket]
}

object OutletsSetter {

  def apply[Out <: NonEmptyTuple](using setter: OutletsSetter[Out]): OutletsSetter[Out] = setter

  given single[Out: NotTuple]: OutletsSetter[Tuple1[Out]] with {
    def convertToData(
      out: Tuple1[Out],
      outlets: TOutlets[Tuple1[Out]],
      outletsMap: Map[Outlet[?], (ControllerId, PeripheryId)]
    ): Chunk[Message.DataPacket] = {
      val outlet  = outlets._1
      val value   = out._1
      val address = outletsMap(outlet)
      Chunk(Message.DataPacket(address._1, address._2, outlet.format(value)))
    }
  }

  given step[H: NotTuple, T <: NonEmptyTuple](using tailSetter: OutletsSetter[T]): OutletsSetter[H *: T] with {
    def convertToData(
      out: H *: T,
      outlets: TOutlets[H *: T],
      outletsMap: Map[Outlet[?], (ControllerId, PeripheryId)]
    ): Chunk[Message.DataPacket] = {
      val value   = out.head
      val outlet  = outlets.head
      val address = outletsMap(outlet)
      Message.DataPacket(address._1, address._2, outlet.format(value)) +:
        tailSetter.convertToData(out.tail, outlets.tail, outletsMap)
    }
  }
}
