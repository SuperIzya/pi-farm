package com.ilyak.pifarm.control.configuration

import akka.stream._
import akka.stream.scaladsl.{Broadcast, Merge}
import cats.kernel.Semigroup
import com.ilyak.pifarm.Build.{BuildResult, FoldResult, TMap}
import com.ilyak.pifarm.Units
import com.ilyak.pifarm.flow.configuration.Connection
import com.ilyak.pifarm.flow.configuration.Connection.{Connect, ConnectF, In, Out}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, CMap, ExternalInputs, ExternalOutputs, Inputs, Outputs}

import scala.language.higherKinds

/** *
  * Helper types used by [[Builder]]
  *
  */
private[configuration] object BuilderHelpers {

  import BuildResult._
  import cats.implicits._

  type TMapGroup[T[_]] = Semigroup[TMap[T[_]]]
  type CompiledGraph = BuildResult[AutomatonConnections]
  type InletMap = TMap[Inlet[_]]
  type OutletMap = TMap[Outlet[_]]
  type ConnectionsMap = ConnectionsCounter[List[AutomatonConnections]]
  type Closed[T[_]] = Either[Connect, T[_]]
  type TMapCGroup[T[_]] = Semigroup[TMap[Closed[T]]]

  implicit val closedInGroup: TMapCGroup[In] = _ ++ _
  implicit val closedOutGroup: TMapCGroup[Out] = _ ++ _

  implicit val inGroup: TMapGroup[In] = _ ++ _
  implicit val outGroup: TMapGroup[Out] = _ ++ _


  def foldResults[S, E](append: (S, E) => S): (BuildResult[S], BuildResult[E]) => BuildResult[S] =
    BuildResult.combine(_, _)(append(_, _))

  def foldResultsT[T](append: (T, T) => T): (BuildResult[T], BuildResult[T]) => BuildResult[T] =
    foldResults[T, T](append)

  implicit val flowIn: Broadcast[Any] => Inlet[_] = _.in
  implicit val flowOut: Merge[Any] => Outlet[_] = _.out

  def foldConnections[L[_], C[_] <: Connection[_, L] : CMap : TMapGroup, S]
  (direction: String,
   allConnections: TMap[List[AutomatonConnections]],
   multi: List[C[_]] => S,
   connect: (S, List[L[_]]) => Connect)
  (implicit let: S => L[_],
   toConnection: ToConnection[L, C]): FoldResult[C[_]] = {
    allConnections.map {
      case (k: String, lst: List[AutomatonConnections]) if lst.size == 1 =>
        CMap[C].apply(lst.head).get(k)
          .map(x => Result(Map(k -> x)))
          .getOrElse(Error(s"No $direction for key $k"))

      case (k: String, c: List[AutomatonConnections]) =>
        c.map(a => CMap[C]
          .apply(a)
          .get(k)
          .map(cc => Result(List(cc)))
          .getOrElse(Error(s"No $direction for key $k in node ${a.node.map(_.id.toString).getOrElse("")}"))
        ).foldLeft[BuildResult[List[C[_]]]](Result(List.empty)) {
          foldResultsT[List[C[_]]](_ |+| _)
        }
          .map(l => multi(l) -> l)
          .map(p => toConnection(
            p._2.head,
            let(p._1),
            connect(p._1, p._2.map(_.let))
          ))
          .map(x => Map(k -> x))
    }.foldLeft[FoldResult[C[_]]](Result(Map.empty)) {
      foldResultsT[TMap[C[_]]](_ |+| _)
    }
  }

  def tryConnect[In[_], Out[_]](k: String, in: In[_], outs: TMap[Out[_]])
                               (implicit connect: ConnectF[In, Out]): FoldResult[Closed[In]]
  = outs
    .get(k)
    .map(out => connect(in, out).map(a => Map(k -> Left(a))))
    .getOrElse(Result(Map(k -> Right(in))))


  def connectAll[I[_] : TMapCGroup, O[_]](ins: TMap[I[_]], outs: TMap[O[_]])
                                         (implicit connect: ConnectF[I, O]): FoldResult[Closed[I]] =
    ins
      .map { case (k, in) => tryConnect(k, in, outs) }
      .foldLeft[FoldResult[Closed[I]]](Result(Map.empty))(
      foldResultsT(_ |+| _)
    )

  def connectExternal[I[_] : TMapCGroup, O[_]](dir: String, ins: TMap[I[_]], outs: TMap[O[_]])
                                              (implicit connect: ConnectF[I, O]): BuildResult[Connect] =
    connectAll(ins, outs).flatMap { m =>
      m.map {
        case (k, Right(_)) => Error(s"$dir '$k' is not connection")
        case (_, Left(c)) => Result(c)
      }.foldLeft[BuildResult[Connect]](Result(Connect.empty))(foldResultsT(_ |+| _))
    }

  implicit class ConnectInputs(val ins: Inputs) extends AnyVal {
    def connect(outs: Outputs): FoldResult[Closed[In]] = connectAll(ins, outs)

    def connectExternals(outs: ExternalInputs): BuildResult[Connect] =
      connectExternal("input", ins, outs)
  }

  implicit class ConnectOutputs(val outs: Outputs) extends AnyVal {
    def connect(ins: Inputs): FoldResult[Closed[Out]] = connectAll(outs, ins)

    def connectExternals(ins: ExternalOutputs): BuildResult[Connect] =
      connectExternal("output", outs, ins)
  }


  trait ToConnection[L[_], C[_] <: Connection[_, L]] {
    def apply(conn: C[_], xlet: L[_], connect: Connect): C[_]
  }

  implicit val inletToIn: ToConnection[Inlet, In] = (conn, xlet, connect) => {
    implicit val u: Units[Any] = new Units[Any] {
      override val name: String = conn.unit
    }
    Connection(conn.name, xlet.as[Any])
  }

  implicit val outletToOut: ToConnection[Outlet, Out] = (conn, xlet, connect) => {
    implicit val u: Units[Any] = new Units[Any] {
      override val name: String = conn.unit
    }
    Connection(conn.name, xlet.as[Any])
  }
}
