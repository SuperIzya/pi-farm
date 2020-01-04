package com.ilyak.pifarm.flow.configuration

import akka.actor.{ActorRef, PoisonPill}
import akka.stream._
import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import cats.Monoid
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Units
import com.ilyak.pifarm.flow.configuration.Connection.TConnection
import com.ilyak.pifarm.types._

import scala.language.higherKinds

sealed trait Connection[T, L[_]] extends TConnection {
  type Let = L[T]
  type GetLet = GState[Let]

  val let: GetLet
}

object Connection {

  import cats.implicits._

  sealed trait TConnection {
    val unit: Units[_]
    val node: String
    val name: String
  }

  type ExtConnectState = (ConnectState, Int)

  case class Sockets(inputs: SMap[Inlet[_]], outputs: SMap[Outlet[_]])

  object Sockets {
    val empty = new Sockets(Map.empty, Map.empty)

    implicit val monoidSockets: Monoid[Sockets] = new Monoid[Sockets] {
      override def empty: Sockets = Sockets.empty

      override def combine(x: Sockets, y: Sockets): Sockets =
        Sockets(x.inputs ++ y.inputs, x.outputs ++ y.outputs)
    }
  }
  trait SocketsToLets[L[_]] {
    def apply(sockets: Sockets): SMap[L[_]]
  }

  object SocketsToLets {
    def apply[T[_]: SocketsToLets]: SocketsToLets[T] =
      implicitly[SocketsToLets[T]]
  }

  implicit val inletSLets: SocketsToLets[Inlet] = _.inputs
  implicit val outletSLets: SocketsToLets[Outlet] = _.outputs

  trait ToSocket[L[_]] {
    def apply(l: L[_], n: String): Sockets
  }

  object ToSocket {
    def apply[L[_]: ToSocket]: ToSocket[L] = implicitly[ToSocket[L]]
  }

  implicit val inletSock: ToSocket[Inlet] = (l, n) =>
    Sockets(Map(n -> l), Map.empty)
  implicit val outletSock: ToSocket[Outlet] = (l, n) =>
    Sockets(Map.empty, Map(n -> l))

  private def tryConnect[C <: TConnection, D <: TConnection](
    x: C,
    y: D,
    connect: ConnectState
  ): Result[ConnectState] = {
    Result.cond(
      Units.areEqual(x.unit, y.unit),
      connect,
      s"Wrong units (${x.name}:${x.unit} -> ${y.name}:${y.unit})"
    )
  }

  sealed trait Put[L[_]] {
    def as[T](l: L[_]): L[T]

    def let[T, F[_] <: Connection[T, L]](f: F[T]): GState[L[T]] =
      f.let

    def lets(s: Sockets): SMap[L[_]]

    def getLet[T](name: String): Sockets => L[T] = s => as[T](lets(s)(name))

    def getState[T](node: String, name: String): GState[L[T]] =
      GraphState.retrieveSockets(node).map(getLet(name))

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
    def apply[F[_] <: Connection[_, Inlet]: Input]: Input[F] =
      implicitly[Input[F]]

    def create[F[_] <: Connection[_, Inlet]]: Input[F] = new Input[F]
  }

  object Output {
    def apply[F[_] <: Connection[_, Outlet]: Output]: Output[F] =
      implicitly[Output[F]]

    def create[F[_] <: Connection[_, Outlet]]: Output[F] = new Output[F]
  }

  def apply[O[_] <: Connection[_, Outlet], I[_] <: Connection[_, Inlet]](
    out: O[_],
    in: I[_]
  )(implicit O: Output[O], I: Input[I]): Result[ConnectState] = {
    import GraphDSL.Implicits._

    val s: ConnectState = for {
      sOut <- out.let
      sIn <- in.let
      res <- GState.pure { implicit b =>
        sOut.as[Any] ~> sIn.as[Any]
        ()
      }
    } yield res

    tryConnect(in, out, s)
  }

  object In {
    implicit val conn: Input[In] = Input.create

    def apply[T: Units](name: String, node: String): In[T] =
      apply(name, node, conn.getState(node, name))

    def apply[T: Units](name: String,
                        node: String,
                        shape: In[T]#GetLet): In[T] =
      new In(name, node, Units[T], shape)
  }

  object Out {
    implicit val conn: Output[Out] = Output.create

    def apply[T: Units](name: String, node: String): Out[T] =
      apply(name, node, conn.getState[T](node, name))

    def apply[T: Units](name: String,
                        node: String,
                        shape: Out[T]#GetLet): Out[T] =
      new Out(name, node, Units[T], shape)
  }

  case class Out[T] private (name: String,
                             node: String,
                             unit: Units[T],
                             let: Out[T]#GetLet)
      extends Connection[T, Outlet]

  case class In[T] private (name: String,
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

      def apply[S[_] <: Shape: MapS]: MapS[S] = implicitly[MapS[S]]

      implicit val src: MapS[SourceShape] = new MapS[SourceShape] {
        override def order[T](kill: Kill[T],
                              s: SourceShape[T]): (Outlet[T], Inlet[T]) =
          s.out -> kill.getInput.as[T]

        override def toOutput[T](kill: Kill[T], name: String): SMap[Outlet[T]] =
          Map(name -> kill.getOut)
      }

      implicit val dst: MapS[SinkShape] = new MapS[SinkShape] {
        override def order[T](kill: Kill[T],
                              s: SinkShape[T]): (Outlet[T], Inlet[T]) =
          kill.getOut -> s.in

        override def toInput[T](kill: Kill[T], name: String): SMap[Inlet[T]] =
          Map(name -> kill.getInput.as[T])
      }
    }

    private def getSockets[T, S[_] <: Shape](
      name: String,
      shape: Graph[S[T], _]
    )(implicit S: MapS[S]): GState[Sockets] = GState { state => implicit b =>
      {
        import GraphDSL.Implicits._
        val kill = b add new KillGuard[T]()
        state.kill ~> kill.getKillIn
        val s = b add shape
        val (out, in) = S.order(kill, s)
        out ~> in
        state -> Sockets(S.toInput(kill, name), S.toOutput(kill, name))
      }
    }

    private def getLet[T, L[_], C <: Connection[T, L], P[_] <: Put[L]](
      node: String,
      name: String,
      put: P[C]
    ): C#GetLet =
      put.getState(node, name)

    private def createGetter[T, L[_], C <: Connection[T, L], P[_] <: Put[L]](
      node: String,
      name: String,
      creator: GState[Sockets]
    )(implicit p: P[C]): C#GetLet =
      for {
        _ <- GraphState.add(node, creator)
        res <- getLet[T, L, C, P](node, name, p)
      } yield res

    object ExtIn {
      implicit val conn: Input[ExtIn] = Input.create

      def apply[T: Units](name: String,
                          node: String,
                          actor: ActorRef): ExtIn[T] =
        apply(name, node, Sink.actorRef(actor, PoisonPill))

      def apply[T: Units](name: String,
                          node: String,
                          sink: Sink[T, _]): ExtIn[T] = {
        val let: ExtIn[T]#GetLet =
          createGetter(node, name, getSockets(name, sink))
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String): ExtIn[T] =
        apply(name, node, conn.getState[T](node, name))

      def apply[T: Units](name: String,
                          node: String,
                          add: ExtIn[T]#GetLet): ExtIn[T] =
        new ExtIn(name, node, Units[T], add)
    }

    object ExtOut {
      implicit val conn: Output[ExtOut] = Output.create

      def apply[T: Units](name: String,
                          node: String,
                          actor: ActorRef): ExtOut[T] =
        apply(
          name,
          node,
          Source
            .actorRef(1, OverflowStrategy.dropHead)
            .mapMaterializedValue(a => {
              actor ! Subscribe(a)
              a
            })
        )

      def apply[T: Units](name: String,
                          node: String,
                          source: Source[T, _]): ExtOut[T] = {
        val let: ExtOut[T]#GetLet =
          createGetter(node, name, getSockets(name, source))
        apply(name, node, let)
      }

      def apply[T: Units](name: String, node: String): ExtOut[T] =
        apply(name, node, conn.getState[T](node, name))

      def apply[T: Units](name: String,
                          node: String,
                          add: ExtOut[T]#GetLet): ExtOut[T] =
        new ExtOut[T](name, node, Units[T], add)
    }

    case class ExtIn[T] private (name: String,
                                 node: String,
                                 unit: Units[T],
                                 let: ExtIn[T]#GetLet)
        extends Connection[T, Inlet]

    case class ExtOut[T] private (name: String,
                                  node: String,
                                  unit: Units[T],
                                  let: ExtOut[T]#GetLet)
        extends Connection[T, Outlet]

  }
}
