package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge}
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream._
import cats.data.Chain
import cats.{Monad, Monoid, ~>}
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.Connection.{Connect, Flw, In, Out}
import com.ilyak.pifarm.flow.configuration.{Connection, TConnection}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, CMap, Inputs, Outputs}

import scala.language.higherKinds

/** *
  * Intermediate types used by [[Builder]]
  *
  */
private[configuration] object BuilderTypes {
  type TMap[T] = Map[String, T]
  type ConnCounter[T] = TMap[T]
  type CompiledGraph = BuildResult[AutomatonConnections]
  type InletMap = TMap[Inlet[_]]
  type OutletMap = TMap[Outlet[_]]
  type ConnectionsMap = ConnectionsCounter[List[AutomatonConnections]]
  type FoldResult[T] = BuildResult[TMap[T]]

  implicit val tmapMonad: Monoid[TMap[_]] = new Monoid[TMap[_]] {
    override def empty: TMap[_] = Map.empty[String, _]

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


  def foldConnections[L[_], C[_] <: Connection[_, L] : CMap, S[_] <: Graph[_ <: Shape, _]]
  (direction: String,
   allConnections: TMap[List[AutomatonConnections]],
   multi: Int => S[_])
  (implicit builder: Builder[_],
   SL: S ~> L,
   CL: C ~> L,
   connect: Connect[S, L]
  ): FoldResult[L[_]] = {

    val cmap = CMap[C]

    allConnections.map {
      case (k: String, List(c)) =>
        cmap(c).get(k)
          .map(x => Right(Map(k -> CL(x))))
          .getOrElse(Left(s"No $direction for key $k"))

      case (k: String, c: List[AutomatonConnections]) =>
        val m = multi(c.size)
        val conn: C[_] => Boolean = a => connect(m, a.let)

        BuildResult.cond(
          c.forall(cmap(_).get(k).exists(conn(_))),
          Map(k -> SL(m)),
          s"Not all ${direction}s can be connected for $direction '$k'"
        )
    }.foldLeft[FoldResult[L[_]]](Right(Map.empty))(
      foldResultsT(_ ++ _)
    )
  }

  def connectAll[In[_] <: TConnection[_], Out[_] <: TConnection[_]]
  (ins: TMap[In[_]], outs: TMap[Out[_]])
  (implicit connect: Connect[In, Out]): BuildResult[TMap[Flw[_]]] =
    ins.map { case (k, in) =>
      outs
        .get(k)
        .map(out => BuildResult.Result(Map(k -> connect(in, out))))
        .getOrElse(BuildResult.Error(s"Not found output for input $k"))
    }.foldLeft[BuildResult[TMap[Flw[_]]]](BuildResult.Result(Map.empty))(
      foldResultsT(_ ++ _)
    )


  implicit class ConnectInputs(val ins: Inputs) extends AnyVal {
    def connect(outs: Outputs)(implicit connect: Connect[In, Out]): BuildResult[TMap[Flw[_]]] =
      connectAll(ins, outs)
  }

  implicit class ConnectOutputs(val outs: Outputs) extends AnyVal {
    def connect(ins: Inputs)(implicit connect: Connect[Out, In]): BuildResult[TMap[Flw[_]]] =
      connectAll(outs, ins)
  }


  // TODO: Find more appropriate place for these implicits
  implicit val bcastLet: Broadcast ~> Inlet = Lambda[Broadcast ~> Inlet](_.in)
  implicit val mergeLet: Merge ~> Outlet = Lambda[Merge ~> Outlet](_.out)

  implicit val bcastConn: Connect[Broadcast, Inlet] = new Connect[Broadcast, Inlet] {
    override def apply(c: Broadcast[_], d: Inlet[_])(implicit b: Builder[_]): Boolean = {
      import GraphDSL.Implicits._
      c.shape ~> d.as[Any]
      true
    }
  }
  implicit val mergeConn: Connect[Merge, Outlet] = new Connect[Merge, Outlet] {
    override def apply(c: Merge[_], d: Outlet[_])(implicit b: Builder[_]): Boolean = {
      import GraphDSL.Implicits._
      d.as[Any] ~> c.asInstanceOf[Merge[Any]].shape
      true
    }
  }

}
