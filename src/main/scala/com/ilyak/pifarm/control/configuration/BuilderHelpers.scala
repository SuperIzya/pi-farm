package com.ilyak.pifarm.control.configuration

import akka.stream._
import cats.kernel.Semigroup
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm._
import com.ilyak.pifarm.flow.configuration.Connection
import com.ilyak.pifarm.flow.configuration.Connection._
import com.ilyak.pifarm.flow.configuration.ShapeConnections._

import scala.annotation.tailrec
import scala.language.higherKinds

/** *
  * Helper types used by [[Builder]]
  *
  */
private[configuration] object BuilderHelpers {

  import BuildResult._
  import State.Implicits._
  import cats.implicits._

  type CompiledGraph = BuildResult[AutomatonConnections]
  type InletMap = SMap[Inlet[_]]
  type OutletMap = SMap[Outlet[_]]
  type ConnectionsMap = ConnectionsCounter[List[AutomatonConnections]]
  type Closed[T[_]] = Either[ConnectShape, T[_]]
  type TMapCGroup[T[_]] = Semigroup[SMap[Closed[T]]]

  implicit val closedInGroup: TMapCGroup[In] = _ ++ _
  implicit val closedOutGroup: TMapCGroup[Out] = _ ++ _

  implicit val inGroup: HKMapGroup[In] = _ ++ _
  implicit val outGroup: HKMapGroup[Out] = _ ++ _


  implicit val flowIn: UniformFanOutShape[Any, Any] => Inlet[_] = _.in
  implicit val flowOut: UniformFanInShape[Any, Any] => Outlet[_] = _.out

  def foldConnections[L[_] : SLets, C[_] <: Connection[_, L] : CMap : HKMapGroup, S <: Shape]
  (direction: String,
   allConnections: SMap[List[AutomatonConnections]],
   multi: List[C[_]] => akka.stream.Graph[S, _],
   connect: (S, L[_]) => GBuilder[Unit])
  (implicit let: S => L[_],
   toSocket: ToSocket[L],
   toConnection: ToConnection[L, C]): FoldResult[C[_]] = {

    val fold: (AutomatonConnections, String) => BuildResult[List[C[_]]] = (a, k) =>
      CMap[C]
        .apply(a)
        .get(k)
        .map(cc => Result(List(cc)))
        .getOrElse(Error(s"No $direction for key $k in node ${ a.node.map(_.id.toString).getOrElse("") }"))

    BuildResult.foldSMap(
      allConnections.map {
        case (k: String, List()) => Error(s"No connections for key $k")
        case (k: String, List(l)) =>
          CMap[C].apply(l).get(k)
                 .map(x => Result(Map(k -> x)))
                 .getOrElse(Error(s"No $direction for key $k"))
        case (k: String, c: List[AutomatonConnections]) =>
          c.map(fold(_, k))
          .foldLeft[BuildResult[List[C[_]]]](Result(List.empty))(foldResultsT(_ |+| _))
          .map(l => {
            val nodes = l.map(_.node)
            val node: String = l.map(n => s"_${n.node}_${n.name}_").foldLeft("")(_ + _)

            val slets = SLets[L]

            val shape: GRun[L[_]] = ss => implicit b => {
              val s = b add multi(l)
              val getLet: (String, Sockets) => L[_] = (k, sc) => slets(sc)(k)
              val (s1, lets) = ss(nodes, getLet)
              lets.foreach(connect(s, _))
              val g: GBuilder[Sockets] = _ => toSocket(s, l.head.name)
              (s1 |+| (node -> g), let(s))
            }
            /// !!!!

            Map(k -> toConnection(l.head, node, slets(_)(l.head.name)))
          })
      }
    )
  }

  def connectAll2(ins: SMap[In[_]], outs: SMap[Out[_]])
                 (implicit connect: ConnectF[In, Out]) = {
    type I = In[_]
    type O = Out[_]
    type Res = (SMap[In[_]], SMap[Out[_]], ConnectShape)
    type SIn = SMap[I]
    type SOut = SMap[O]

    @tailrec
    def _conn(in: SIn, out: SOut, collect: Res): BuildResult[Res] = {
      if (in.isEmpty) BuildResult.Result(collect.copy(_2 = collect._2 ++ out))
      else if (out.isEmpty) BuildResult.Result(collect.copy(_1 = collect._1 ++ in))
      else {
        val (k, i) = in.head

        val o = out.get(k)
        if (o.isDefined) {
          val c = connect(i, o.get)
          if (c.isLeft) BuildResult.Error(c.left.get)
          else _conn(in - k, out - k, collect.copy(_3 = collect._3 |+| c.right.get))
        }
        else _conn(in - k, out, collect.copy(_1 = collect._1 + (k -> i)))
      }
    }

    val e: Res = (Map.empty, Map.empty, ConnectShape.empty)
    _conn(ins, outs, e)
  }

  def connectAll[I[_] : TMapCGroup, O[_]](ins: SMap[I[_]], outs: SMap[O[_]])
                                         (implicit connect: ConnectF[I, O]): FoldResult[Closed[I]] = {
    val tryConnect: (String, I[_]) => FoldResult[Closed[I]] = (k, in) =>
      outs
      .get(k)
      .map(out => connect(in, out).map(a => Map(k -> Left(a))))
      .getOrElse(Result(Map(k -> Right(in))))

    BuildResult.foldSMap(ins.map { case (k, in) => tryConnect(k, in) })
  }

  def connectExternal[I[_] : TMapCGroup, O[_]](dir: String,
                                               ins: SMap[I[_]],
                                               outs: SMap[O[_]])
                                              (implicit connect: ConnectF[I, O]): BuildResult[ConnectShape] = {
    val map: SMap[Closed[I]] => TraversableOnce[BuildResult[ConnectShape]] = _.map {
      case (k, Right(_)) => Error(s"$dir '$k' is not connection")
      case (_, Left(c)) => Result(c)
    }

    connectAll(ins, outs).flatMap { m => BuildResult.foldAll(map(m)) }
  }

  implicit class ConnectInputs(val ins: Inputs) extends AnyVal {
    def connect(outs: Outputs): FoldResult[Closed[In]] = connectAll(ins, outs)

    def connectExternals(outs: ExternalInputs): BuildResult[ConnectShape] =
      connectExternal("input", ins, outs)
  }

  implicit class ConnectOutputs(val outs: Outputs) extends AnyVal {
    def connect(ins: Inputs): FoldResult[Closed[Out]] = connectAll(outs, ins)

    def connectExternals(ins: ExternalOutputs): BuildResult[ConnectShape] =
      connectExternal("output", outs, ins)
  }

  trait ToConnection[L[_], C[_] <: Connection[_, L]] {
    def apply(conn: C[_], node: String, xlet: Sockets => L[_]): C[_]
  }

  implicit val inletToIn: ToConnection[Inlet, In] = (conn, node, xlet) => {
    implicit val u: Units[Any] = new Units[Any] {
      override val name: String = conn.unit
    }
    Connection.In(conn.name, node, xlet)
  }

  implicit val outletToOut: ToConnection[Outlet, Out] = (conn, node, xlet) => {
    implicit val u: Units[Any] = new Units[Any] {
      override val name: String = conn.unit
    }
    Connection.Out(conn.name, node, xlet)
  }

  trait SLets[L[_]] {
    def apply(sockets: Sockets): SMap[L[_]]
  }

  object SLets {
    def apply[T[_] : SLets]: SLets[T] = implicitly[SLets[T]]
  }

  implicit val inletSLets: SLets[Inlet] = _.inputs
  implicit val outletSLets: SLets[Outlet] = _.outputs

  trait ToSocket[L[_]] {
    def apply(l: L[_], n: String): Sockets
  }

  implicit val inletSock: ToSocket[Inlet] = (l, n) => Sockets(Map(n -> l), Map.empty)
  implicit val outletSock: ToSocket[Outlet] = (l, n) => Sockets(Map.empty, Map(n -> l))

}
