package com.ilyak.pifarm

import cats.Monoid
import com.ilyak.pifarm.Types.{ GBuilder, GraphBuilder, SMap }
import com.ilyak.pifarm.flow.configuration.Connection.{ ConnectShape, Sockets }

import scala.language.implicitConversions


object State {
  type GSockets = GBuilder[Sockets]

  case class GraphState(map: SMap[Sockets], creators: SMap[GSockets])

  object GraphState {
    val empty = GraphState(Map.empty, Map.empty)
  }

  object Implicits {
    implicit def toStateOps(t: GraphState): StateOpsC = new StateOpsC(t)

    implicit val monoid: Monoid[ConnectShape] = new Monoid[ConnectShape] {
      override def empty: ConnectShape = s => _ => (s, Unit)

      override def combine(x: ConnectShape, y: ConnectShape): ConnectShape = s => b => {
        val (s1, _) = x(s)(b)
        val (s2, _) = y(s1)(b)
        (s2, Unit)
      }
    }
  }

  final class StateOpsC(lhs: GraphState) {
    def |+|(rhs: (String, GBuilder[Sockets])): GraphState =
      lhs.copy(creators = lhs.creators + rhs)

    def apply(key: String)(implicit b: GraphBuilder): (GraphState, Sockets) =
      lhs.map
      .get(key).map(v => (lhs, v))
      .getOrElse({
        val v = lhs.creators(key)(b)
        (lhs.copy(map = lhs.map ++ Map(key -> v), creators = lhs.creators - key), v)
      })

    def apply[R](key: String, f: Sockets => (GraphState, R))
                (implicit b: GraphBuilder): (GraphState, R) = {
      val (s, v) = apply(key)
      f(v)
    }

    def apply[R](keys: Seq[String], f: (String, Sockets) => R)
                (implicit b: GraphBuilder): (GraphState, Seq[R]) = {

      def _app(k: Seq[String], coll: Seq[R]): GraphState => (GraphState, Seq[R]) = t => {
        if (k.isEmpty) (t, coll)
        else {
          val (t1, r) = t(k.head, f(k.head, _))
          _app(k.tail, coll ++ r)(t1)
        }
      }

      _app(keys, Seq.empty)(lhs)
    }
  }

}
