package com.ilyak.pifarm.control.configuration

import akka.stream._
import akka.stream.scaladsl.Flow
import cats.{Monoid, ~>}
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

  type ConnCounter[T] = TMap[T]
  type CompiledGraph = BuildResult[AutomatonConnections]
  type InletMap = TMap[Inlet[_]]
  type OutletMap = TMap[Outlet[_]]
  type ConnectionsMap = ConnectionsCounter[List[AutomatonConnections]]
  type Closed[T[_]] = Either[Connected, T[_]]

  implicit val tmapMonad: Monoid[TMap[_]] = new Monoid[TMap[_]] {
    override def empty: TMap[_] = Map.empty[String, Any]

    override def combine(x: TMap[_], y: TMap[_]): TMap[_] = x ++ y
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


  def foldConnections[L[_], C[_] <: Connection[_, L] : CMap]
  (direction: String,
   allConnections: TMap[List[AutomatonConnections]],
   multi: List[L[_]] => Flow[_, _, _])
  (SL: Flow ~> L,
   CL: C ~> L): FoldResult[L[_]] = {
    import cats.implicits._

    val cmap = CMap[C]

    allConnections.map {
      case (k: String, List(c)) =>
        cmap(c).get(k)
          .map(x => Right(Map(k -> CL(x))))
          .getOrElse(Left(s"No $direction for key $k"))

      case (k: String, c: List[AutomatonConnections]) =>
        c.map(
          cmap(_)
            .get(k)
            .map(cc => BuildResult.Result(List(cc.let)))
            .getOrElse(BuildResult.Error(s"No $direction for key $k"))
        ).foldLeft[BuildResult[List[L[_]]]](Right(List.empty))(
          foldResultsT(_ |+| _)
        ).map(multi(_))
          .map(m => Map(k -> SL(m)))

    }.foldLeft[FoldResult[L[_]]](Right(Map.empty))(
      foldResultsT(_ ++ _)
    )
  }

  def tryConnect[In[_], Out[_]](k: String, in: In[_], outs: TMap[Out[_]])
                               (implicit connect: Connect[In, Out]): FoldResult[Closed[In]]
  = outs
    .get(k)
    .map(out => connect(in, out).map(a => Map(k -> Left(a))))
    .getOrElse(BuildResult.Result(Map(k -> Right(in))))


  def connectAll[In[_], Out[_]](dir: String,
                                revDir: String,
                                ins: TMap[In[_]],
                                outs: TMap[Out[_]])
                               (implicit connect: Connect[In, Out]): FoldResult[Closed[In]] =
    ins
      .map { case (k, in) => tryConnect(k, in, outs) }
      .foldLeft[FoldResult[Closed[In]]](BuildResult.Result(Map.empty))(
      foldResultsT(_ ++ _)
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
