package org.pi.farm

import zio.stream.{ZChannel, ZStream}

import scala.annotation.implicitNotFound
import org.pi.farm.model.Address
import scala.util.{NotGiven, TupledFunction}
import zio.{Task, ZIO}
import zio.json.ast.Json
import org.pi.farm.model.Message
import org.pi.farm.model.Message.DataPacket

package object plugin {
  type NotTuple[T] = NotGiven[T <:< Tuple]
  type =:!=[A, B]  = NotGiven[A =:= B]

  type Stream[T]        = ZStream[Any, Throwable, T]
  type Channel[In, Out] = ZChannel[Any, Throwable, In, Any, Throwable, Out, Any]

  @implicitNotFound(msg = "Apparently ${From} <:< ${To}.")
  sealed trait <:!<[-From, +To]
  given [From, To](using NotGiven[From <:< To]): <:!<[From, To] = new <:!<[From, To] {}

  type MapToAddress[T <: Tuple] = Tuple.Map[T, [x] =>> Address]

  type AddressFrom[In] = In match {
    case x *: y => Address *: MapToAddress[y]
    case _      => Address
  }

  type TypeOrDataPacket[T] <: Message = T match {
    case DataPacket               => DataPacket
    case Message.Command          => Message.Command
    case Message.Measurement      => Message.Measurement
    case Message.Error            => Message.Error
    case Message.Discovery        => Message.Discovery
    case Message.ServerDiscovered => Message.ServerDiscovered
    case Message.Ping             => Message.Ping
    case Message.Pong             => Message.Pong
    case _                        => DataPacket
  }

  type Description = Json
}
