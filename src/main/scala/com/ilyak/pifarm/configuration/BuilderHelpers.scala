package com.ilyak.pifarm.configuration

import akka.stream._
import cats.Monoid
import cats.kernel.Semigroup
import com.ilyak.pifarm._
import com.ilyak.pifarm.flow.configuration.Connection
import com.ilyak.pifarm.flow.configuration.Connection.External.{ExtIn, ExtOut}
import com.ilyak.pifarm.flow.configuration.Connection._
import com.ilyak.pifarm.flow.configuration.ShapeConnections._
import com.ilyak.pifarm.types._

import scala.annotation.tailrec
import scala.language.higherKinds

/** *
  * Helper types used by [[Builder]]
  *
  */
private[configuration] object BuilderHelpers {

  import cats.implicits._
  import com.ilyak.pifarm.types.Result._

  type CompiledGraph = Result[AutomatonConnections]
  type InletMap = SMap[Inlet[_]]
  type OutletMap = SMap[Outlet[_]]
  type ConnectionsMap = ConnectionsCounter[List[AutomatonConnections]]
  type Closed[T[_]] = Either[ConnectState, T[_]]
  type TMapCGroup[T[_]] = Semigroup[SMap[Closed[T]]]

  implicit val closedInGroup: TMapCGroup[In] = _ ++ _
  implicit val closedOutGroup: TMapCGroup[Out] = _ ++ _
  implicit val closedExtOutGroup: TMapCGroup[External.ExtOut] = _ ++ _

  implicit val inGroup: HKMapGroup[In] = _ ++ _
  implicit val outGroup: HKMapGroup[Out] = _ ++ _

  implicit val flowIn: UniformFanOutShape[Any, Any] => Inlet[_] = _.in
  implicit val flowOut: UniformFanInShape[Any, Any] => Outlet[_] = _.out

  // @formatter:off
  def foldConnections[L[_] : SocketsToLets : ToSocket,
                      C[_] <: Connection[_, L] : ShapesConnections : HKMapGroup,
                      S <: Shape]
  // @formatter:on
  (direction: String,
   allEdges: SMap[List[AutomatonConnections]],
   multi: Int => akka.stream.Graph[S, _],
   connect: (S, L[_]) => GBuilder[Unit])(
    implicit let: S => L[_],
    toConnection: ToConnection[L, C]
  ): FoldResult[C[_]] = {

    val fold: (AutomatonConnections, String) => Result[List[C[_]]] = (a, k) =>
      ShapesConnections[C]
        .get(a)
        .get(k)
        .map(cc => Res(List(cc)))
        .getOrElse(
          Err(
            s"No $direction for key $k in node ${a.node.map(_.id.toString).getOrElse("")}"
          )
      )
    val shapeBuilder: Int => GState[S] = size =>
      GState.pure { implicit b =>
        val shapeMulti = b add multi(size)
        shapeMulti
    }

    def plexConnections(name: String, node: String, l: List[C[_]])(
      implicit toSocket: ToSocket[L],
      slets: SocketsToLets[L]
    ): GState[L[_]] = {
      val size = l.size

      def unType[T](t: L[T]): L[_] = t

      val builderState: GState[Sockets] = for {
        shape <- shapeBuilder(size)
        connectLet = connect(shape, _)
        lets <- l.map(c => c.let.map(unType(_))).sequence
        _ <- lets.map(connectLet).map(GState.pure).sequence
        res <- GState.pure(GBuilder.pure(toSocket(let(shape), name)))
      } yield res

      for {
        _ <- GraphState.add(node, builderState)
        sock <- GraphState.retrieveSockets(node)
        res <- GState.pure(GBuilder.pure(slets(sock)(name)))
      } yield res
    }

    Result.foldSMap(allEdges.map {
      case (k: String, List()) => Err(s"No connections for key $k")
      case (k: String, List(l)) =>
        ShapesConnections[C]
          .get(l)
          .get(k)
          .map(x => Res(Map(k -> x)))
          .getOrElse(Err(s"No $direction for key $k"))
      case (k: String, c: List[AutomatonConnections]) =>
        c.map(fold(_, k))
          .combineAll
          .map(l => {
            val name = l.head.name
            val node: String =
              l.map(n => s"_${n.node}_${n.name}_").mkString

            val state: GState[L[_]] = plexConnections(name, node, l)
            Map(k -> toConnection(l.head, node, state))
          })
    })
  }

  def interConnect(
    ins: SMap[In[_]],
    outs: SMap[Out[_]]
  ): Result[(SMap[In[_]], SMap[Out[_]], ConnectState)] = {
    type I = In[_]
    type O = Out[_]
    type Res = (SMap[In[_]], SMap[Out[_]], ConnectState)
    type SIn = SMap[I]
    type SOut = SMap[O]

    @tailrec
    def _conn(in: SIn, out: SOut, collect: Res): Result[Res] = {
      (in, out) match {
        case (l, _) if l.isEmpty => Res(collect.copy(_2 = collect._2 ++ out))
        case (_, l) if l.isEmpty => Res(collect.copy(_1 = collect._1 ++ in))
        case _ =>
          val (k, i) = in.head

          out.get(k) match {
            case Some(o) =>
              Connection(o, i) match {
                case Left(c) => Err(c)
                case Right(c) =>
                  _conn(in - k, out - k, collect.copy(_3 = collect._3 |+| c))
              }
            case None =>
              _conn(in - k, out, collect.copy(_1 = collect._1 + (k -> i)))
          }
      }
    }

    val e: Res = (Map.empty, Map.empty, Monoid[ConnectState].empty)
    _conn(ins, outs, e)
  }

  def tryConnect[F[_]: TMapCGroup, G[_]](
    ones: SMap[F[_]],
    many: SMap[G[_]],
    f: (F[_], G[_]) => Result[ConnectState]
  ): FoldResult[Closed[F]] =
    Result.foldSMap(ones.map {
      case (name, one) =>
        many
          .get(name)
          .map(f(one, _).map(a => Map(name -> a)))
          .getOrElse(Res(Map(name -> ConnectState.empty)))
          .map(_.get(name) match {
            case None                     => Map(name -> Right(one))
            case Some(ConnectState.empty) => Map(name -> Right(one))
            case Some(a)                  => Map(name -> Left(a))
          })
    })

  def connectExternal[F[_]: TMapCGroup, G[_]](
    dir: String,
    fs: SMap[F[_]],
    gs: SMap[G[_]],
    connect: (F[_], G[_]) => Result[ConnectState]
  ): Result[ExtConnectState] = {
    val map: SMap[Closed[F]] => TraversableOnce[Result[ConnectState]] =
      _.map {
        case (k, Right(_)) => Err(s"$dir '$k' is not connection")
        case (_, Left(c))  => Res(c)
      }
    def comb(e: ExtConnectState, c: ConnectState): ExtConnectState =
      (e._1 |+| c) -> (e._2 + 1)

    val init: ExtConnectState = ConnectState.empty -> 0
    tryConnect[F, G](fs, gs, connect)
      .flatMap { m =>
        Result.fold(map(m))(init, comb)
      }
  }

  implicit class ConnectInputs(val ins: Inputs) extends AnyVal {
    def connect(outs: Outputs): FoldResult[Closed[In]] =
      tryConnect[In, Out](ins, outs, (i, o) => Connection(o, i))

    def connectExternals(outs: ExternalOutputs): Result[ExtConnectState] =
      connectExternal[In, ExtOut](
        "input",
        ins,
        outs,
        (i, o) => Connection(o, i)
      )
  }

  implicit class ConnectOutputs(val outs: Outputs) extends AnyVal {

    def connect(ins: Inputs): FoldResult[Closed[Out]] =
      tryConnect[Out, In](outs, ins, Connection(_, _))

    def connectExternals(ins: ExternalInputs): Result[ExtConnectState] =
      connectExternal[Out, ExtIn]("output", outs, ins, Connection(_, _))
  }

  trait ToConnection[L[_], C[_] <: Connection[_, L]] {
    def apply(conn: C[_], node: String, xlet: GState[L[_]]): C[Any]
  }

  implicit val inletToIn: ToConnection[Inlet, In] = (conn, node, xlet) => {
    implicit val u: Units[Any] = conn.unit.name

    Connection.In(conn.name, node, xlet.map(_.as[Any]))
  }

  implicit val outletToOut: ToConnection[Outlet, Out] = (conn, node, xlet) => {
    implicit val u: Units[Any] = conn.unit.name

    Connection.Out(conn.name, node, xlet.map(_.as[Any]))
  }
}
