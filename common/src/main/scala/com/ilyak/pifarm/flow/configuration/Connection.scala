package com.ilyak.pifarm.flow.configuration

import akka.actor.{ ActorRef, PoisonPill }
import akka.stream._
import akka.stream.scaladsl.{ GraphDSL, Sink, Source }
import cats.Monoid
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.flow.configuration.Connection.TConnection
import com.ilyak.pifarm.{ Result, Units }

import scala.language.higherKinds


sealed trait Connection[T, L[_]] extends TConnection {
  type Let = L[T]
  type GetLet = GRun[Let]

  val let: GetLet
}

object Connection {

  import com.ilyak.pifarm.State.Implicits._
  import cats.implicits._

  sealed trait TConnection {
    val unit: Units[_]
    val node: String
    val name: String
  }

  type ConnectShape = GRun[Unit]
  type ExtConnectShape = (ConnectShape, Int)

  case class Sockets(inputs: Map[String, Inlet[_]], outputs: Map[String, Outlet[_]])

  object Sockets {
    val empty = new Sockets(Map.empty, Map.empty)
  }

  implicit val monoidSockets: Monoid[Sockets] = new Monoid[Sockets] {
    override def empty: Sockets = Sockets.empty

    override def combine(x: Sockets, y: Sockets): Sockets =
      Sockets(x.inputs ++ y.inputs, x.outputs ++ y.outputs)
  }


  trait SocketsToLets[L[_]] {
    def apply(sockets: Sockets): SMap[L[_]]
  }

  object SocketsToLets {
    def apply[T[_] : SocketsToLets]: SocketsToLets[T] = implicitly[SocketsToLets[T]]
  }

  implicit val inletSLets: SocketsToLets[Inlet] = _.inputs
  implicit val outletSLets: SocketsToLets[Outlet] = _.outputs

  trait ToSocket[L[_]] {
    def apply(l: L[_], n: String): Sockets
  }

  object ToSocket {
    def apply[L[_] : ToSocket]: ToSocket[L] = implicitly[ToSocket[L]]
  }

  implicit val inletSock: ToSocket[Inlet] = (l, n) => Sockets(Map(n -> l), Map.empty)
  implicit val outletSock: ToSocket[Outlet] = (l, n) => Sockets(Map.empty, Map(n -> l))

  object ConnectShape {
    val empty: ConnectShape = s => _ => (s, Unit)

    private def tryConnect[C <: TConnection, D <: TConnection]
      (x: C, y: D, connect: ConnectShape): Result[ConnectShape] = {
      Result.cond(
        Units.areEqual(x.unit, y.unit),
        connect,
        s"Wrong units (${ x.name }:${ x.unit } -> ${ y.name }:${ y.unit })"
      )
    }

    def apply(in: In[_], out: Out[_]): Result[ConnectShape] = {
      import GraphDSL.Implicits._
      val c: ConnectShape = state => implicit b => {
        val (st1, sOut) = out.let(state)(b)
        val (st2, sIn) = in.let(st1)(b)
        sOut.as[Any] ~> sIn.as[Any]
        (st2, Unit)
      }
      tryConnect(out, in, c)
    }

    def apply(out: Out[_], in: In[_]): Result[ConnectShape] = apply(in, out)

    def apply(in: In[_], extIn: External.Out[_]): Result[ConnectShape] = {
      import GraphDSL.Implicits._
      val c: ConnectShape = ss => implicit b => {
        val (s1, sOut) = extIn.let(ss)(b)
        val (s2, sIn) = in.let(s1)(b)
        val kill = b add new KillGuard()
        sOut.as[Any] ~> kill ~> sIn.as[Any]
        s2.kill ~> kill
        (s2, Unit)
      }
      tryConnect(in, extIn, c)
    }

    def apply(out: Out[_], extOut: External.In[_]): Result[ConnectShape] = {
      import GraphDSL.Implicits._
      val c: ConnectShape = state => implicit b => {
        val (st1, o) = out.let(state)(b)
        val (st2, i) = extOut.let(st1)(b)
        val kill = b add new KillGuard()
        o.as[Any] ~> kill ~> i.as[Any]
        st2.kill ~> kill
        (st2, Unit)
      }
      tryConnect(out, extOut, c)
    }

    implicit val monad: Monoid[ConnectShape] = new Monoid[ConnectShape] {
      override def empty: ConnectShape = ConnectShape.empty

      override def combine(x: ConnectShape, y: ConnectShape): ConnectShape = (x, y) match {
        case (ConnectShape.empty, ConnectShape.empty) => ConnectShape.empty
        case (ConnectShape.empty, a) => a
        case (a, ConnectShape.empty) => a
        case (c, d) =>
          val res: ConnectShape = ss => b => {
            val (s1, _) = c(ss)(b)
            val (s2, _) = d(s1)(b)
            (s2, Unit)
          }
          res
      }
    }
  }


  object In {
    def apply[T: Units](name: String, node: String): In[T] =
      apply(name, _.inputs(name).as[T], node)


    def apply[T: Units](name: String, get: Sockets => In[T]#Let, node: String): In[T] = {
      val let: In[T]#GetLet = ss => implicit b => ss(node, get)
      apply(name, node, let)
    }

    def apply[T: Units](name: String, node: String, shape: In[T]#GetLet): In[T] =
      new In(name, node, Units[T], shape(_))
  }

  object Out {
    def apply[T: Units](name: String, node: String): Out[T] = apply(name, _.outputs(name).as[T], node)

    def apply[T: Units](name: String, get: Sockets => Out[T]#Let, node: String): Out[T] = {
      val let: Out[T]#GetLet = ss => implicit b => ss(node, get(_))
      apply(name, node, let)
    }

    def apply[T: Units](name: String, node: String, shape: Out[T]#GetLet): Out[T] =
      new Out(name, node, Units[T], shape(_))
  }

  case class Out[T] private(name: String,
                            node: String,
                            unit: Units[T],
                            let: Out[T]#GetLet)
    extends Connection[T, Outlet]

  case class In[T] private(name: String,
                           node: String,
                           unit: Units[T],
                           let: In[T]#GetLet)
    extends Connection[T, Inlet]

  object External {

    object In {
      def apply[T: Units](name: String, node: String, actor: ActorRef): In[T] =
        apply(name, node, Sink.actorRef(actor, PoisonPill))

      def apply[T: Units](name: String, node: String, sink: Sink[T, _]): In[T] = {
        val run: GRun[Sockets] = ss => bb => {
          val dst = bb add sink
          (ss, Sockets(Map(name -> dst.in), Map.empty))
        }
        val let: In[T]#GetLet = state => implicit b => {
          val (s1, scs) = state.getOrElse(node, run)
          scs.inputs.get(name)
            .map(o => (s1, o.as[T]))
            .getOrElse {
              val (s2, soc) = run(s1)(b)
              val (s3, soc2) = s2.replace(node, soc |+| scs)
              (s3, soc2.inputs(name).as[T])
            }
        }
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String): In[T] = {
        val let: In[T]#GetLet = ss => implicit b => ss(node, _.inputs(name).as[T])
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String, add: In[T]#GetLet): In[T] =
        new In(name, node, Units[T], add(_))
    }

    object Out {
      def apply[T: Units](name: String, node: String, actor: ActorRef): Out[T] =
        apply(name, node, Source.actorRef(1, OverflowStrategy.dropHead).
          mapMaterializedValue(a => {
            actor ! Subscribe(a)
            a
          }))

      def apply[T: Units](name: String, node: String, source: Source[T, _]): Out[T] = {
        val run: GRun[Sockets] = ss => bb => {
          val dst = bb add source
          (ss, Sockets(Map.empty, Map(name -> dst.out)))
        }
        val let: Out[T]#GetLet = state => implicit b => {
          val (s1, scs) = state.getOrElse(node, run)
          scs.outputs.get(name)
            .map(o => (s1, o.as[T]))
            .getOrElse {
              val (s2, soc) = run(s1)(b)
              val (s3, soc2) = s2.replace(node, soc |+| scs)
              (s3, soc2.outputs(name).as[T])
            }
        }
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String): Out[T] = {
        val let: Out[T]#GetLet = ss => implicit b => ss(node, _.outputs(name).as[T])
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String, add: Out[T]#GetLet): Out[T] =
        new Out[T](name, node, Units[T], add)
    }

    case class In[T] private(name: String,
                             node: String,
                             unit: Units[T],
                             let: In[T]#GetLet)
      extends Connection[T, Inlet]

    case class Out[T] private(name: String,
                              node: String,
                              unit: Units[T],
                              let: Out[T]#GetLet)
      extends Connection[T, Outlet]

  }

  trait ConnectF[C[_], D[_]] {
    def apply(c: C[_], d: D[_]): Result[ConnectShape]
  }

  implicit val Cio: ConnectF[In, Out] = ConnectShape(_, _)
  implicit val Coi: ConnectF[Out, In] = ConnectShape(_, _)
  implicit val Ceio: ConnectF[In, External.Out] = ConnectShape(_, _)
  implicit val Ceoi: ConnectF[Out, External.In] = ConnectShape(_, _)
}

