package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.{Broadcast, Merge}
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream._
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.Connection
import com.ilyak.pifarm.flow.configuration.Connection.{TCon, TLet, XLet}
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

  trait SLet[S[_] <: Graph[_ <: S, _], L[_]] {
    def apply(s: S[_]): L[_]
  }

  implicit val inLet: SLet[SinkShape, Inlet] = _.in
  implicit val bcastLet: SLet[Broadcast, Inlet] = _.in
  implicit val outLet: SLet[SourceShape, Outlet] = _.out
  implicit val mergeLet: SLet[Merge, Outlet] = _.out




  def foldConnections[C : CMap, L: XLet[C, _], S <: Graph[_ <: S, _]]
  (direction: String,
   allConnections: TMap[List[AutomatonConnections]],
   multi: Int => S,
   connect: (S, C) => Unit)
  (implicit builder: Builder[_], sLet: SLet[S, L]): FoldResult[L] = {

    val cmap = CMap[C]
    val tlet = TLet[C, L]

    allConnections.map {
      case (k: String, List(c)) => cmap(c).get(k)
        .map(x => Right(Map(k -> tlet(x))))
        .getOrElse(Left(s"No $direction for key $k"))

      case (k: String, c: List[AutomatonConnections]) => {
        val m = builder.add(multi(c.size))

        BuildResult.cond[TMap[L]](
          c.forall(cmap(_).get(k).exists(a => {
            connect(m, a)
            true
          })),
          Map(k -> sLet(m)),
          s"Not all ${direction}s can be connected for $direction $k"
        )
      }
    }.foldLeft[FoldResult[L]](Right(Map.empty))(
      foldResults[TMap[L], TMap[L]](_ ++ _)
    )

  }

  def areAllConnected[In, Out](ins: TMap[In],
                               outs: TMap[Out],
                               connection: (In, Out) => Boolean): Boolean =
    ins.forall {
      case (k: String, input: In) => outs.get(k).exists(out => connection(input, out))
    }


}
