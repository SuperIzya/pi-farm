package com.ilyak.pifarm.control.configuration

import akka.stream._
import akka.stream.scaladsl.Flow
import cats.kernel.Semigroup
import cats.~>
import com.ilyak.pifarm.Build.{BuildResult, FoldResult, TMap}
import com.ilyak.pifarm.flow.configuration.Connection
import com.ilyak.pifarm.flow.configuration.Connection.{Connect, Connected, In, Out}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, CMap, Inputs, Outputs}

import scala.language.higherKinds

/** *
  * Helper types used by [[Builder]]
  *
  */
private[configuration] object BuilderHelpers {

  import cats.implicits._

  type Flw[T] = Flow[T, T, _]
  type ConnCounter[T] = TMap[T]
  type TMapGroup[T[_]] = Semigroup[TMap[T[_]]]
  type CompiledGraph = BuildResult[AutomatonConnections]
  type InletMap = TMap[Inlet[_]]
  type OutletMap = TMap[Outlet[_]]
  type ConnectionsMap = ConnectionsCounter[List[AutomatonConnections]]
  type Closed[T[_]] = Either[Connected, T[_]]
  type TMapCGroup[T[_]] = Semigroup[TMap[Closed[T]]]

  implicit val inGroup: TMapGroup[Inlet] = _ ++ _
  implicit val outGroup: TMapGroup[Outlet] = _ ++ _
  implicit val closedInGroup: TMapCGroup[In] = _ ++ _
  implicit val closedOutGroup: TMapCGroup[Out] = _ ++ _

  object Flw {
    def apply(): Flw[_] = Flow[Any]

    def apply[T](): Flw[T] = Flow[T]
  }


  def foldResults[S, E](append: (S, E) => S): (BuildResult[S], BuildResult[E]) => BuildResult[S] = {
    case (Right(se), Right(el)) => Right(append(se, el))
    case (Left(l1), Left(l2)) => Left(
      s"""
         |$l1
         |$l2
            """.stripMargin)
    case (l@Left(_), _) => l
    case (_, Left(l)) => Left(l)
  }

  def foldResultsT[T](append: (T, T) => T): (BuildResult[T], BuildResult[T]) => BuildResult[T] =
    foldResults[T, T](append)


  implicit val flowIn: Flw ~> Inlet = Lambda[Flw ~> Inlet](_.shape.in)
  implicit val flowOut: Flw ~> Outlet = Lambda[Flw ~> Outlet](_.shape.out)

  def foldConnections[L[_] : TMapGroup, C[_] <: Connection[_, L] : CMap]
  (direction: String,
   allConnections: TMap[List[AutomatonConnections]],
   multi: List[L[_]] => Flw[_])
  (implicit SL: Flw ~> L,
   CL: C ~> L): FoldResult[L[_]] = {

    allConnections.map {
      case (k: String, lst: List[AutomatonConnections]) if lst.size == 1 =>
        CMap[C].apply(lst.head).get(k)
          .map(x => BuildResult.Result(Map(k -> CL(x))))
          .getOrElse(BuildResult.Error(s"No $direction for key $k"))

      case (k: String, c: List[AutomatonConnections]) =>
        c.map(a => CMap[C]
          .apply(a)
          .get(k)
          .map(cc => BuildResult.Result(List(cc.let)))
          .getOrElse(BuildResult.Error(s"No $direction for key $k in node ${a.node.id}"))

        ).foldLeft[BuildResult[List[L[_]]]](Right(List.empty)) {
          foldResultsT[List[L[_]]](_ |+| _)
        }
          .map(multi(_))
          .map[TMap[L[_]]](m => Map(k -> SL(m)))

    }.foldLeft[FoldResult[L[_]]](Right(Map.empty)) {
      foldResultsT[TMap[L[_]]](_ |+| _)
    }
  }

  def tryConnect[In[_], Out[_]](k: String, in: In[_], outs: TMap[Out[_]])
                               (implicit connect: Connect[In, Out]): FoldResult[Closed[In]]
  = outs
    .get(k)
    .map(out => connect(in, out).map(a => Map(k -> Left(a))))
    .getOrElse(BuildResult.Result(Map(k -> Right(in))))


  def connectAll[In[_]: TMapCGroup, Out[_]](dir: String,
                                revDir: String,
                                ins: TMap[In[_]],
                                outs: TMap[Out[_]])
                               (implicit connect: Connect[In, Out]): FoldResult[Closed[In]] =
    ins
      .map { case (k, in) => tryConnect(k, in, outs) }
      .foldLeft[FoldResult[Closed[In]]](BuildResult.Result(Map.empty))(
      foldResultsT(_ |+| _)
    )

  implicit class ConnectInputs(val ins: Inputs) extends AnyVal {
    def connect(outs: Outputs)(implicit connect: Connect[In, Out]): FoldResult[Closed[In]] =
      connectAll("input", "output", ins, outs)
  }

  implicit class ConnectOutputs(val outs: Outputs) extends AnyVal {
    def connect(ins: Inputs)(implicit connect: Connect[Out, In]): FoldResult[Closed[Out]] =
      connectAll("output", "input", outs, ins)
  }

}
