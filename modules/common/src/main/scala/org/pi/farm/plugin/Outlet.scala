package org.pi.farm.plugin

import izumi.reflect.Tag
import zio.json._
import zio.json.ast.Json
import org.pi.farm.model.Message.DataPacket
import org.pi.farm.model.*
import org.pi.farm.model.given
import zio.Chunk
import scala.language.implicitConversions

trait Outlet[Out] {
  trait Configured {
    def name: Name
    def encode(out: Out): Chunk[DataPacket]
  }

  def description: Description
  def configure(addresses: AddressFrom[Out]): this.Configured
}

object Outlet {
  given scalar: [A]
    => (tag: Tag[A])
    => (NotTuple[A])
    => (AddressFrom[A] =:= Address)
    => (JsonEncoder[A]) => Outlet[A] {

    def description: Description = Json.Obj("tag" -> Json.Str(tag.tag.toString))

    def configure(address: AddressFrom[A]): this.Configured = new Configured {
      val name: Name = s"${tag.tag.toString}: ${address.name} (${address.controllerId}, ${address.peripheryId})"
      def encode(a: A): Chunk[DataPacket] =
        Chunk.single(
          DataPacket(
            address.controllerId,
            address.peripheryId,
            a.toJsonAST.toOption.get
          )
        )
    }
  }

  given tuple2: [A, B]
    => (tagA: Tag[A])
    => (tagB: Tag[B])
    => (A: Outlet[A])
    => (B: Outlet[B])
    => (AddressFrom[(A, B)] =:= (Address, Address))
    => (Address =:= AddressFrom[A])
    => (Address =:= AddressFrom[B])
    => (NotTuple[A])
    => (NotTuple[B]) => OutletTuple[(A, B)] {

    def descriptions: Chunk[Description] = Chunk(A.description, B.description)

    def configure(address: AddressFrom[(A, B)]): this.ConfiguredTuple = new ConfiguredTuple {
      val confA = A.configure(address._1)
      val confB = B.configure(address._2)

      val names: List[Name] = List(tagA.tag.toString, tagB.tag.toString)

      def encode(out: (A, B)): Chunk[DataPacket] = confA.encode(out._1) ++ confB.encode(out._2)
    }
  }

  given incTuple: [A <: Tuple, B]
    => (A: OutletTuple[A])
    => (B: Outlet[B])
    => (tagB: Tag[B])
    => (AddressFrom[B *: A] =:= Address *: MapToAddress[A])
    => (Address =:= AddressFrom[B])
    => (MapToAddress[A] =:= AddressFrom[A])
    => (NotTuple[B]) => OutletTuple[B *: A] {

    def descriptions: Chunk[Description] = Chunk(B.description) ++ A.descriptions

    def configure(addresses: AddressFrom[B *: A]): this.ConfiguredTuple = new ConfiguredTuple {
      val confA = A.configure(addresses.tail)
      val confB = B.configure(addresses.head)

      val names: List[Name] = confA.names.prepended(tagB.tag.toString)

      def encode(out: B *: A): Chunk[DataPacket] =
        confB.encode(out.head) ++ confA.encode(out.tail)
    }
  }

  sealed trait OutletTuple[A <: Tuple] extends Outlet[A] {

    def description: Description = Json.Arr(descriptions)
    private[Outlet] def descriptions: Chunk[Description]

    def configure(addresses: AddressFrom[A]): ConfiguredTuple
    trait ConfiguredTuple extends Configured {
      def name: Name = names.mkString("[", ", ", "]")

      private[Outlet] def names: List[Name]
    }
  }

  given Outlet[Unit] with {
    def description: Description = Json.Null

    def configure(address: AddressFrom[Unit]): this.Configured = new Configured {
      val name: Name                         = "UnitOutlet"
      def encode(a: Unit): Chunk[DataPacket] = Chunk.empty
    }
  }
}
