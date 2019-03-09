package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge}
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream._
import cats.~>
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.Connection.Connect
import com.ilyak.pifarm.flow.configuration.{Connection, TConnection}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, CMap}

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

        BuildResult.cond(
          c.forall(cmap(_).get(k).exists(a => {
            connect(m, a.let)
            true
          })),
          Map(k -> SL(m)),
          s"Not all ${direction}s can be connected for $direction $k"
        )
    }.foldLeft[FoldResult[L[_]]](Right(Map.empty))(
      foldResults[TMap[L[_]], TMap[L[_]]](_ ++ _)
    )
  }

  def connectAll[In[_] <: TConnection[_], Out[_] <: TConnection[_]](ins: TMap[In[_]],
                                                                    outs: TMap[Out[_]])
                                                                   (implicit connect: Connect[In, Out],
                                                                    b: GraphDSL.Builder[_]): Boolean =
    ins.forall(x => outs
      .get(x._1)
      .exists(out => {
        connect(x._2, out)
        true
      })
    )



  // TODO: Find more appropriate place for these implicits
  implicit val bcastLet: Broadcast ~> Inlet = Lambda[Broadcast ~> Inlet](_.in)
  implicit val mergeLet: Merge ~> Outlet = Lambda[Merge ~> Outlet](_.out)

  implicit val bcastConn: Connect[Broadcast, Inlet] = new Connect[Broadcast, Inlet] {
    import GraphDSL.Implicits._
    override def apply(c: Broadcast[_], d: Inlet[_])(implicit b: Builder[_]): Unit =
      c.shape ~> d.as[Any]
  }
  implicit val mergeConn: Connect[Merge, Outlet] = new Connect[Merge, Outlet] {
    import GraphDSL.Implicits._
    override def apply(c: Merge[_], d: Outlet[_])(implicit b: Builder[_]): Unit =
      d.as[Any] ~> c.asInstanceOf[Merge[Any]].shape
  }
}
