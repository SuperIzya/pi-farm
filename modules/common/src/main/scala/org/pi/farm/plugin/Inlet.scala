package org.pi.farm.plugin

import cats.syntax.all.*
import izumi.reflect.Tag
import org.pi.farm.model.Message.DataPacket
import org.pi.farm.model.{ControllerId, PeripheryId, Name, Address, Message}
import org.pi.farm.model.given
import zio.json.*
import zio.json.ast.Json
import zio.*
import scala.language.implicitConversions

import scala.util.NotGiven
import zio.json.ast.Json
import javax.xml.crypto.Data
import org.pi.farm.model.Message.Measurement
import org.pi.farm.model.Message.Command
import org.pi.farm.model.Message.Discovery
import org.pi.farm.model.Message.ServerDiscovered
import org.pi.farm.model.Message.Ping
import org.pi.farm.model.Message.Pong
import org.pi.farm.model.Message.Inbound
import scala.reflect.TypeTest

trait Inlet[In] { self =>
  type Env
  def init: URIO[Scope, Env]

  def getValue(env: Env): UIO[Option[In]]

  def configure(config: AddressFrom[In]): self.Configured

  trait Configured {
    def name: Name
    def setValue(env: Env, value: Inbound): UIO[Unit]
  }

  def description: Description
}

object Inlet {

  sealed trait InletTuple[A <: Tuple] extends Inlet[A] {
    type Env <: Tuple

    def configure(config: AddressFrom[A]): ConfiguredTuple

    def description: Description = Json.Arr(descriptions)

    private[Inlet] def descriptions: Chunk[Description]

    trait ConfiguredTuple extends Configured {
      val name: Name = names.mkString("[", ", ", "]")

      def names: List[Name]
    }
  }

  given scalarDataPacket: [A]
    => (A: Tag[A])
    => (NotTuple[A])
    => (AddressFrom[A] =:= Address)
    => (TypeOrDataPacket[A] =:= DataPacket)
    => (JsonDecoder[A]) => Inlet[A] { self =>
    type Env = Ref[Option[A]]
    val init: UIO[Env] = Ref.make(None)

    val description: Description = Json.Obj("tag" -> Json.Str(A.tag.toString))

    def getValue(env: Env): UIO[Option[A]] = env.get

    def configure(config: AddressFrom[A]): self.Configured = {
      val peripheryId  = config.peripheryId
      val controllerId = config.controllerId
      val name         = config.name

      new Configured {
        val name: Name = s"${A.tag} ($controllerId, $peripheryId)"

        def setValue(env: Env, value: Inbound): UIO[Unit] =
          value match {
            case value @ DataPacket(controllerId, peripheryId, data)
                if controllerId == controllerId && peripheryId == peripheryId =>
              ZIO
                .fromEither(data.as[A])
                .asSome
                .catchAll(e => ZIO.logError(s"Error parsing data packet $name: $e") *> ZIO.none)
                .flatMap(env.set)
                .unit
            case _ => ZIO.unit
          }
      }
    }
  }

  given scalarMessage: [A <: Message]
    => (A: Tag[A])
    => (tt: TypeTest[Inbound, A])
    => (NotTuple[A])
    => (AddressFrom[A] =:= Address)
    => (NotGiven[DataPacket =:= A])
    => (TypeOrDataPacket[A] =:= A)
    => (JsonDecoder[A]) => Inlet[A] { self =>
    type Env = Ref[Option[A]]
    val init: UIO[Env] = Ref.make(None)

    val description: Description = Json.Obj("tag" -> Json.Str(A.tag.toString))

    def getValue(env: Env): UIO[Option[A]] = env.get

    def configure(config: AddressFrom[A]): self.Configured = {
      val pId  = config.peripheryId
      val cId  = config.controllerId
      val name = config.name

      new Configured {
        val name: Name = s"${A.tag} ($cId, $pId)"

        private val testInbound: PartialFunction[Inbound, A] = {
          case tt(a) => a
        }
        private val testA: PartialFunction[A, A] = {
          case m: Measurement if m.controllerId == cId   => m
          case e: Message.Error if e.controllerId == cId => e
          case d: Discovery if d.controllerId == cId     => d
          case p: Ping if p.controllerId == cId          => p
        }
        private val test = testInbound.andThen(testA)

        def setValue(env: Env, value: Inbound): UIO[Unit] =
          test.lift(value).fold(ZIO.unit)(v => env.set(Some(v)).unit)
      }
    }
  }
  given genTuple2: [A: NotTuple, B: NotTuple]
    => (A: Inlet[A])
    => (B: Inlet[B])
    => (AddressFrom[(A, B)] =:= (Address, Address))
    => (Address =:= AddressFrom[A])
    => (Address =:= AddressFrom[B]) => InletTuple[(A, B)] { self =>
    type Env = (A.Env, B.Env)
    val init: URIO[Scope, (A.Env, B.Env)] = A.init.zipWith(B.init)(_ -> _)

    private[Inlet] def descriptions: Chunk[Description] = Chunk(A.description, B.description)

    def getValue(env: (A.Env, B.Env)): UIO[Option[(A, B)]] =
      for {
        a <- A.getValue(env._1)
        b <- B.getValue(env._2)
      } yield (a, b).mapN(_ -> _)

    def configure(config: AddressFrom[(A, B)]): self.ConfiguredTuple = {
      val cA = A.configure(config._1)
      val cB = B.configure(config._2)

      new ConfiguredTuple {
        val names: List[Name] = List(cA.name, cB.name)

        def setValue(env: Env, value: Inbound): UIO[Unit] =
          cA.setValue(env._1, value) *>
            cB.setValue(env._2, value)
      }
    }
  }

  given incTuple: [A <: Tuple, B: NotTuple]
    => (A: InletTuple[A])
    => (B: Inlet[B])
    => (AddressFrom[B *: A] =:= Address *: MapToAddress[A])
    => (Address =:= AddressFrom[B])
    => (MapToAddress[A] =:= AddressFrom[A]) => InletTuple[B *: A] { self =>
    type Env = B.Env *: A.Env
    val init: URIO[Scope, Env] = B.init.zipWith(A.init)(_ *: _)

    def descriptions: Chunk[Description] = Chunk(B.description) ++ A.descriptions

    def configure(config: AddressFrom[B *: A]): self.ConfiguredTuple = {
      val cA = A.configure(config.tail)
      val cB = B.configure(config.head)
      new ConfiguredTuple {
        val names: List[Name] = cB.name :: cA.names

        def setValue(env: Env, value: Inbound): UIO[Unit] =
          cB.setValue(env.head, value) *>
            cA.setValue(env.tail, value)
      }
    }

    def getValue(env: B.Env *: A.Env): UIO[Option[B *: A]] =
      for {
        b <- B.getValue(env.head)
        a <- A.getValue(env.tail)
      } yield (b, a).mapN(_ *: _)
  }

  given unit: Inlet[Unit] { self =>
    type Env = Unit
    val init: UIO[Env] = ZIO.unit

    def description: Description = Json.Null

    def getValue(env: Env): UIO[Option[Unit]] = ZIO.none

    def configure(config: AddressFrom[Unit]): self.Configured = new Configured {
      val name: Name = "Unit Inlet"

      def setValue(env: Env, value: Inbound): UIO[Unit] = ZIO.unit
    }
  }
}
