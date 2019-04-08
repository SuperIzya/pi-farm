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

  def foldConnections[L[_] : SLets : ToSocket, C[_] <: Connection[_, L] : CMap : HKMapGroup, S <: Shape]
  (direction: String,
   allConnections: SMap[List[AutomatonConnections]],
   multi: Int => akka.stream.Graph[S, _],
   connect: (S, L[_]) => GBuilder[Unit])
  (implicit let: S => L[_],
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
            val size = l.size
            val name = l.head.name
            val nodes = l.map(_.node)
            val node: String = l.map(n => s"_${n.node}_${n.name}_").foldLeft("")(_ + _)

            val slets = SLets[L]
            val toSocket = ToSocket[L]

            val sockets: GRun[Sockets] = state => implicit b => {
              val shapeMulti = b add multi(size)
              val getLet: Sockets => L[_] = slets(_)(k)
              val (st1, lets) = state(nodes, getLet)
              lets.foreach(connect(shapeMulti, _)(b))
              (st1, toSocket(let(shapeMulti), name))
            }

            Map(k -> toConnection(l.head, node, state => implicit b => {
              val (st1, scs) = state.getOrElse(node, sockets)
              (st1, slets(scs)(name))
            }))
          })
      }
    )
  }

  def interConnect(ins: SMap[In[_]], outs: SMap[Out[_]])
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
    def apply(conn: C[_], node: String, xlet: GRun[L[_]]): C[Any]
  }

  implicit val inletToIn: ToConnection[Inlet, In] = (conn, node, xlet) => {
    implicit val u: Units[Any] = new Units[Any] {
      override val name: String = conn.unit
    }
    Connection.In(conn.name, node, xlet.map(_.as[Any]))
  }

  implicit val outletToOut: ToConnection[Outlet, Out] = (conn, node, xlet) => {
    implicit val u: Units[Any] = new Units[Any] {
      override val name: String = conn.unit
    }
    Connection.Out(conn.name, node, xlet.map(_.as[Any]))
  }

}
