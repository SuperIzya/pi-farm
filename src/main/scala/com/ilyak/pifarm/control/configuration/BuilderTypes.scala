package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.{Graph, Inlet, Outlet}
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.Connection
import com.ilyak.pifarm.flow.configuration.Connection.XLet
import com.ilyak.pifarm.flow.configuration.ShapeConnections.AutomatonConnections

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


  implicit class ConnectionsCounterOps(val m: ConnCounter[Int]) extends AnyVal {
    def substract(outer: ConnCounter[Int], ex: TMap[_]): ConnCounter[Int] = {
      val external = ex.keys.toSeq

      def _sub(v: (String, Int)) = {
        val (key, value) = v
        val outerVal = outer.getOrElse(key, if (external.contains(key)) value else 0)
        key -> (value - outerVal)
      }

      m.map(_sub)
    }

    def filterOpen: ConnCounter[Int] = m.filter(_._2 != 0)

    def prettyPrint: String = m.toString()
  }

  def foldResults[Seed, Elem](append: (Seed, Elem) => Seed): (BuildResult[Seed], BuildResult[Elem]) => BuildResult[Seed] = {
    case (Right(se), Right(el)) => Right(append(se, el))
    case (Left(l1), Left(l2)) => Left(
      s"""
         |$l1
         |$l2
            """.stripMargin)
    case (l@Left(_), _) => l
    case (_, Left(l)) => Left(l)
  }


  def foldConnections[C <: Connection[_, _], TLet, TShape <: Graph[_ <: TShape, _]]
  (direction: String,
   allConnections: TMap[List[AutomatonConnections]],
   connectionMap: AutomatonConnections => TMap[C],
   multi: List[AutomatonConnections] => TShape,
   connect: (TShape, C) => Boolean,
   xlet: TShape => TLet)
  (implicit builder: Builder[_],
   xLet: XLet[C, TLet]): FoldResult[TLet] = {
    allConnections.map {
      case (k: String, List(c)) => connectionMap(c).get(k)
        .map(x => Right(Map(k -> xLet(x))))
        .getOrElse(Left(s"No $direction for key $k"))
      case (k: String, c: List[AutomatonConnections]) => {
        val m = builder.add(multi(c))

        BuildResult.cond[TMap[TLet]](
          c.forall(connectionMap(_).get(k).exists(connect(m, _))),
          Map(k -> xlet(m)),
          s"Not all ${direction}s can be connected for $direction $k"
        )
      }
    }.foldLeft[FoldResult[TLet]](Right(Map.empty))(
      foldResults[TMap[TLet], TMap[TLet]](_ ++ _)
    )
  }

  def areAllConnected[In, Out](ins: TMap[In],
                               outs: TMap[Out],
                               connection: (In, Out) => Boolean): Boolean =
    ins.forall {
      case (k: String, input: In) => outs.get(k).exists(out => connection(input, out))
    }


}
