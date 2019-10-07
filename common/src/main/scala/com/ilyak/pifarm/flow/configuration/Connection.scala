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
import com.ilyak.pifarm.{ Result, Units }

import scala.language.higherKinds

sealed trait Connection[T, L[_]] extends TConnection {
  type Let = L[T]
  type GetLet = GRun[Let]

  val let: GetLet
}

object Connection {

  import cats.implicits._
  import com.ilyak.pifarm.State.Implicits._

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

    sealed trait Put[L[_]] {
      def as[T](l: L[_]): L[T]

      def let[T, F[_] <: Connection[_, L]](f: F[T], state: GraphState, b: GraphBuilder): (GraphState, L[T]) =
        f.let(state)(b).map(as[T])

      def lets(s: Sockets): SMap[L[_]]

      def getLet[T](name: String): Sockets => L[T] = s => as[T](lets(s)(name))

      def getRun[T](node: String, name: String): GRun[L[T]] =
        s => implicit b => s(node, getLet[T](name))
    }

    final class Input[F[_] <: Connection[_, Inlet]] extends Put[Inlet] {
      override def as[T](l: Inlet[_]): Inlet[T] = l.as[T]

      override def lets(s: Sockets): SMap[Inlet[_]] = s.inputs
    }

    final class Output[F[_] <: Connection[_, Outlet]] extends Put[Outlet] {
      override def as[T](l: Outlet[_]): Outlet[T] = l.as[T]

      override def lets(s: Sockets): SMap[Outlet[_]] = s.outputs
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
    implicit val conn: Input[In] = Input.create

    def apply[T: Units](name: String, node: String): In[T] =
      apply(name, node, conn.getRun[T](node, name))

    def apply[T: Units](name: String, node: String, shape: In[T]#GetLet): In[T] =
      new In(name, node, Units[T], shape(_))
  }

  object Out {
    implicit val conn: Output[Out] = Output.create

    def apply[T: Units](name: String, node: String): Out[T] =
      apply(name, node, conn.getRun[T](node, name))

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

    import KillGuard._

    type Kill[T] = UniformFanInShape[Any, T]

    trait MapS[S[_] <: Shape] {
      def order[T](kill: Kill[T], s: S[T]): (Outlet[T], Inlet[T])

      def toInput[T](kill: Kill[T], name: String): SMap[Inlet[T]] = Map.empty

      def toOutput[T](kill: Kill[T], name: String): SMap[Outlet[T]] = Map.empty
    }

    object MapS {

      def apply[S[_] <: Shape : MapS]: MapS[S] = implicitly[MapS[S]]

      implicit val src: MapS[SourceShape] = new MapS[SourceShape] {
        override def order[T](kill: Kill[T], s: SourceShape[T]): (Outlet[T], Inlet[T]) =
          s.out -> kill.getInput.as[T]

        override def toOutput[T](kill: Kill[T], name: String): SMap[Outlet[T]] =
          Map(name -> kill.getOut)
      }

      implicit val dst: MapS[SinkShape] = new MapS[SinkShape] {
        override def order[T](kill: Kill[T], s: SinkShape[T]): (Outlet[T], Inlet[T]) =
          kill.getOut -> s.in

        override def toInput[T](kill: Kill[T], name: String): SMap[Inlet[T]] =
          Map(name -> kill.getInput.as[T])
      }
    }

    private def getSockets[T, S[_] <: Shape](name: String, shape: Graph[S[T], _])
                                            (implicit mapS: MapS[S]): GRun[Sockets] =
      state =>
        implicit b => {
          import GraphDSL.Implicits._
          val kill = b add new KillGuard[T]()
          state.kill ~> kill.getKillIn
          val s = b add shape
          val (out, in) = mapS.order(kill, s)
          out ~> in
          state -> Sockets(mapS.toInput(kill, name), mapS.toOutput(kill, name))
        }

    private def getLet[T, L[_]](run: GRun[Sockets], node: String, name: String, put: Put[L]): GRun[L[T]] =
      state => implicit b => {
        val (s1, scs) = state.getOrCreate(node, run)
        put.lets(scs).get(name)
          .map(o => (s1, o))
          .getOrElse {
            val (s2, soc) = run(s1)(b)
            val (s3, soc2) = s2.replace(node, soc |+| scs)
            (s3, put.lets(soc2)(name))
          }
          .map(put.as[T])
      }

    object ExtIn {
      implicit val conn: Input[ExtIn] = Input.create

      def apply[T: Units](name: String, node: String, actor: ActorRef): ExtIn[T] =
        apply(name, node, Sink.actorRef(actor, PoisonPill))

      def apply[T: Units](name: String, node: String, sink: Sink[T, _]): ExtIn[T] = {
        val run: GRun[Sockets] = getSockets(name, sink)
        val let: ExtIn[T]#GetLet = getLet(run, node, name, conn)
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String): ExtIn[T] =
        apply(name, node, conn.getRun[T](node, name))

      def apply[T: Units](name: String, node: String, add: ExtIn[T]#GetLet): ExtIn[T] =
        new ExtIn(name, node, Units[T], add(_))
    }

    object ExtOut {
      implicit val conn: Output[ExtOut] = Output.create

      def apply[T: Units](name: String, node: String, actor: ActorRef): ExtOut[T] =
        apply(name, node, Source.actorRef(1, OverflowStrategy.dropHead).
          mapMaterializedValue(a => {
            actor ! Subscribe(a)
            a
          }))

      def apply[T: Units](name: String, node: String, source: Source[T, _]): ExtOut[T] = {
        val run: GRun[Sockets] = getSockets(name, source)
        val let: ExtOut[T]#GetLet = getLet(run, node, name, conn)
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String): ExtOut[T] =
        apply(name, node, conn.getRun[T](node, name))

      def apply[T: Units](name: String, node: String, add: ExtOut[T]#GetLet): ExtOut[T] =
        new ExtOut[T](name, node, Units[T], add)
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
