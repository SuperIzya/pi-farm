package com.ilyak.pifarm.flow.configuration

import akka.actor.{ ActorRef, PoisonPill }
import akka.stream._
import akka.stream.scaladsl.{ GraphDSL, Sink, Source }
import cats.Monoid
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.State.GraphState
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.flow.configuration.Connection.ConnectShape.{ Input, Output, Put }
import com.ilyak.pifarm.flow.configuration.Connection.TConnection
import com.ilyak.pifarm.{ Result, State, Units }

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

  case class Sockets(inputs: SMap[Inlet[_]], outputs: SMap[Outlet[_]])

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

    sealed trait Put[L[_], F[_] <: Connection[_, L]] {
      def as[T](l: L[_]): L[T]

      def let[T](f: F[T], state: GraphState, b: GraphBuilder): (GraphState, L[T]) =
        f.let(state)(b).map(as[T])
    }

    final class Input[F[_] <: Connection[_, Inlet]] extends Put[Inlet, F] {
      override def as[T](l: Inlet[_]): Inlet[T] = l.as[T]
    }

    final class Output[F[_] <: Connection[_, Outlet]] extends Put[Outlet, F] {
      override def as[T](l: Outlet[_]): Outlet[T] = l.as[T]
    }

    object Input {
      def apply[F[_] <: Connection[_, Inlet] : Input]: Input[F] = implicitly[Input[F]]

      def create[F[_] <: Connection[_, Inlet]]: Input[F] = new Input[F]
    }

    object Output {
      def apply[F[_] <: Connection[_, Outlet] : Output]: Output[F] = implicitly[Output[F]]

      def create[F[_] <: Connection[_, Outlet]]: Output[F] = new Output[F]
    }

    def apply[O[_] <: Connection[_, Outlet] : Output, I[_] <: Connection[_, Inlet] : Input]
      (out: O[_], in: I[_]): Result[ConnectShape] = {
      import GraphDSL.Implicits._
      val c: ConnectShape = state => implicit b => {
        val (st1, sOut) = Output[O].let(out, state, b)
        val (st2, sIn) = Input[I].let(in, st1, b)
        sOut.as[Any] ~> sIn.as[Any]
        (st2, Unit)
      }
      tryConnect(in, out, c)
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

    implicit val conn: Input[In] = Input.create
  }

  object Out {
    def apply[T: Units](name: String, node: String): Out[T] = apply(name, _.outputs(name).as[T], node)

    def apply[T: Units](name: String, get: Sockets => Out[T]#Let, node: String): Out[T] = {
      val let: Out[T]#GetLet = ss => implicit b => ss(node, get(_))
      apply(name, node, let)
    }

    def apply[T: Units](name: String, node: String, shape: Out[T]#GetLet): Out[T] =
      new Out(name, node, Units[T], shape(_))

    implicit val conn: Output[Out] = Output.create
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

    import KillGuard._

    private def addKill[T](state: State.GraphState)(implicit b: GraphDSL.Builder[_]): UniformFanInShape[Any, T] = {
      import GraphDSL.Implicits._
      val kill = b add new KillGuard[T]()
      state.kill ~> kill.getKillIn
      kill
    }

    def getLet[T, L[_]](run: GRun[Sockets],
                        node: String,
                        name: String,
                        get: Sockets => SMap[L[_]],
                        as: L[_] => L[T]): GRun[L[T]] =
      state => implicit b => {
        val (s1, scs) = state.getOrElse(node, run)
        get(scs).get(name)
          .map(o => (s1, o))
          .getOrElse {
            val (s2, soc) = run(s1)(b)
            val (s3, soc2) = s2.replace(node, soc |+| scs)
            (s3, get(soc2)(name))
          }
          .map(as)
      }

    object ExtIn {
      def apply[T: Units](name: String, node: String, actor: ActorRef): ExtIn[T] =
        apply(name, node, Sink.actorRef(actor, PoisonPill))

      def apply[T: Units](name: String, node: String, sink: Sink[T, _]): ExtIn[T] = {
        val run: GRun[Sockets] = ss => implicit bb => {
          import GraphDSL.Implicits._
          val dst = bb add sink
          val kill = addKill[T](ss)
          kill.getOut ~> dst.in
          (ss, Sockets(Map(name -> kill.getInput), Map.empty))
        }
        val let: ExtIn[T]#GetLet = getLet[T, Inlet](run, node, name, _.inputs, _.as[T])
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String): ExtIn[T] = {
        val let: ExtIn[T]#GetLet = ss => implicit b => ss(node, _.inputs(name).as[T])
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String, add: ExtIn[T]#GetLet): ExtIn[T] =
        new ExtIn(name, node, Units[T], add(_))

      implicit val conn: Input[ExtIn] = Input.create
    }

    object ExtOut {
      def apply[T: Units](name: String, node: String, actor: ActorRef): ExtOut[T] =
        apply(name, node, Source.actorRef(1, OverflowStrategy.dropHead).
          mapMaterializedValue(a => {
            actor ! Subscribe(a)
            a
          }))

      def apply[T: Units](name: String, node: String, source: Source[T, _]): ExtOut[T] = {
        val run: GRun[Sockets] = ss => implicit bb => {
          import GraphDSL.Implicits._
          val dst = bb add source
          val kill = addKill[T](ss)
          dst ~> kill.getInput
          (ss, Sockets(Map.empty, Map(name -> kill.getOut)))
        }
        val let: ExtOut[T]#GetLet = getLet[T, Outlet](run, node, name, _.outputs, _.as[T])
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String): ExtOut[T] = {
        val let: ExtOut[T]#GetLet = ss => implicit b => ss(node, _.outputs(name).as[T])
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String, add: ExtOut[T]#GetLet): ExtOut[T] =
        new ExtOut[T](name, node, Units[T], add)

      implicit val conn: Output[ExtOut] = Output.create
    }

    case class ExtIn[T] private(name: String,
                                node: String,
                                unit: Units[T],
                                let: ExtIn[T]#GetLet)
      extends Connection[T, Inlet]

    case class ExtOut[T] private(name: String,
                                 node: String,
                                 unit: Units[T],
                                 let: ExtOut[T]#GetLet)
      extends Connection[T, Outlet]

  }

}

