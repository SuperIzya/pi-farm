package com.ilyak.pifarm.control.configuration

import akka.stream._
import akka.stream.scaladsl.{Broadcast, Merge}
import cats.kernel.Semigroup
import com.ilyak.pifarm.Build.{BuildResult, FoldResult, TMap, HTMapGroup}
import com.ilyak.pifarm.Units
import com.ilyak.pifarm.flow.configuration.Connection
import com.ilyak.pifarm.flow.configuration.Connection.{ConnectShape, ConnectF, In, Out}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, CMap, ExternalInputs, ExternalOutputs, Inputs, Outputs}

import scala.language.higherKinds

/** *
  * Helper types used by [[Builder]]
  *
  */
private[configuration] object BuilderHelpers {

  import BuildResult._
  import cats.implicits._

  type CompiledGraph = BuildResult[AutomatonConnections]
  type InletMap = TMap[Inlet[_]]
  type OutletMap = TMap[Outlet[_]]
  type ConnectionsMap = ConnectionsCounter[List[AutomatonConnections]]
  type Closed[T[_]] = Either[ConnectShape, T[_]]
  type TMapCGroup[T[_]] = Semigroup[TMap[Closed[T]]]

  implicit val closedInGroup: TMapCGroup[In] = _ ++ _
  implicit val closedOutGroup: TMapCGroup[Out] = _ ++ _

  implicit val inGroup: HTMapGroup[In] = _ ++ _
  implicit val outGroup: HTMapGroup[Out] = _ ++ _


  implicit val flowIn: Broadcast[Any] => Inlet[_] = _.in
  implicit val flowOut: Merge[Any] => Outlet[_] = _.out

  def foldConnections[L[_], C[_] <: Connection[_, L] : CMap : HTMapGroup, S]
  (direction: String,
   allConnections: TMap[List[AutomatonConnections]],
   multi: List[C[_]] => S,
   connect: (S, List[L[_]]) => ConnectShape)
  (implicit let: S => L[_],
   toConnection: ToConnection[L, C]): FoldResult[C[_]] = {

    val fold: (AutomatonConnections, String) => BuildResult[List[C[_]]] = (a, k) =>
      CMap[C]
        .apply(a)
        .get(k)
        .map(cc => Result(List(cc)))
        .getOrElse(Error(s"No $direction for key $k in node ${a.node.map(_.id.toString).getOrElse("")}"))

    BuildResult.foldHTMap(
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
              val m = multi(l)
              val c = toConnection(
                l.head,
                let(m),
                connect(m, l.map(_.let))
              )
              Map(k -> c)
            })
      }
    )
  }

  def connectAll[I[_] : TMapCGroup, O[_]](ins: TMap[I[_]], outs: TMap[O[_]])
                                         (implicit connect: ConnectF[I, O]): FoldResult[Closed[I]] = {
    val tryConnect: (String, I[_]) => FoldResult[Closed[I]] = (k, in) =>
      outs
        .get(k)
        .map(out => connect(in, out).map(a => Map(k -> Left(a))))
        .getOrElse(Result(Map(k -> Right(in))))

    BuildResult.foldTMap(ins.map{case (k, in) => tryConnect(k, in)})
  }

  def connectExternal[I[_] : TMapCGroup, O[_]](dir: String,
                                               ins: TMap[I[_]],
                                               outs: TMap[O[_]])
                                              (implicit connect: ConnectF[I, O]): BuildResult[ConnectShape] =
    connectAll(ins, outs).flatMap { m =>
      BuildResult.fold[ConnectShape, ConnectShape](m.map {
        case (k, Right(_)) => Error(s"$dir '$k' is not connection")
        case (_, Left(c)) => Result(c)
      })(ConnectShape.empty, _ |+| _)
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
    def apply(conn: C[_], xlet: L[_], connect: ConnectShape): C[_]
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
