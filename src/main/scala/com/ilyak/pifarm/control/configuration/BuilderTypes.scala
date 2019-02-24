package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.{Graph, Inlet, Outlet}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.AutomatonConnections

/** *
  * Intermediate types used by [[Builder]]
  *
  */
private[configuration] object BuilderTypes {
  type LString = List[String]
  type PLString = (LString, LString)
  type ConnCounter[T] = Map[String, T]
  type BuildResult[T] = Either[String, T]
  type CompiledGraph = BuildResult[AutomatonConnections]
  type InletMap = Map[String, Inlet[_]]
  type OutletMap = Map[String, Outlet[_]]
  type ConnectionsMap = ConnectionsCounter[List[AutomatonConnections]]


  implicit class ConnectionsCounterOps(val m: ConnCounter[Int]) extends AnyVal {
    def substract(outer: ConnCounter[Int], ex: Map[String, _]): ConnCounter[Int] = {
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


  def foldConnections[Socket, XLet, TShape <: Graph[_ <: TShape, _]]
  (direction: String,
   getSockets: AutomatonConnections => Map[String, Socket],
   connections: Map[String, List[AutomatonConnections]],
   getXLet: Socket => XLet,
   multi: List[AutomatonConnections] => TShape,
   connect: (TShape, Socket) => Boolean,
   xlet: TShape => XLet)
  (implicit builder: Builder[_]): BuildResult[Map[String, XLet]] = {
    connections.map {
      case (k: String, List(c)) => getSockets(c).get(k)
        .map(x => Right(Map(k -> getXLet(x))))
        .getOrElse(Left(s"No $direction for key $k"))
      case (k: String, c: List[AutomatonConnections]) => {
        val m = builder.add(multi(c))

        if (c.forall(getSockets(_).get(k).exists(connect(m, _))))
          Right(Map(k -> xlet(m)))
        else Left(s"Not all ${direction}s can be connected for $direction $k")
      }

    }.foldLeft[BuildResult[Map[String, XLet]]](Right(Map.empty))(
      foldResults[Map[String, XLet], Map[String, XLet]](_ ++ _)
    )
  }

  def areAllConnected[In, Out](ins: Map[String, In],
                               outs: Map[String, Out],
                               connection: (In, Out) => Boolean): Boolean =
    ins.forall {
      case (k: String, input: In) => outs.get(k).exists(out => connection(input, out))
    }


}
