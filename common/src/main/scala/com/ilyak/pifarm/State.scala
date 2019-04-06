package com.ilyak.pifarm

import cats.Monoid
import com.ilyak.pifarm.Types.{ GBuilder, GRun, GraphBuilder, SMap }
import com.ilyak.pifarm.flow.configuration.Connection.{ ConnectShape, Sockets }

import scala.language.implicitConversions


object State {
  type GSockets = GBuilder[Sockets]

  case class GraphState(map: SMap[Sockets], creators: SMap[GSockets])

  object GraphState {
    val empty = GraphState(Map.empty, Map.empty)
  }

  object Implicits {
    implicit val monoid: Monoid[ConnectShape] = new Monoid[ConnectShape] {
      override def empty: ConnectShape = s => _ => (s, Unit)

      override def combine(x: ConnectShape, y: ConnectShape): ConnectShape = s => b => {
        val (s1, _) = x(s)(b)
        val (s2, _) = y(s1)(b)
        (s2, Unit)
      }
    }

    implicit final class StateOpsC(val lhs: GraphState) extends AnyVal {

      def |+|(rhs: (String, GBuilder[Sockets])): GraphState =
        lhs.copy(creators = lhs.creators + rhs)

      def apply(key: String)(implicit b: GraphBuilder): (GraphState, Sockets) =
        lhs.map
        .get(key).map(v => (lhs, v))
        .getOrElse({
          val v = lhs.creators(key)(b)
          (lhs.copy(map = lhs.map ++ Map(key -> v), creators = lhs.creators - key), v)
        })

      def apply[R](key: String, f: Sockets => R)
                  (implicit b: GraphBuilder): (GraphState, R) = {
        val (s, v) = apply(key)
        (s, f(v))
      }

      def apply[R](f: GraphState => R): R = f(lhs)

      def apply[R](keys: Seq[String], f: (String, Sockets) => R)
                  (implicit b: GraphBuilder): (GraphState, Seq[R]) = {

        def _app(k: Seq[String], coll: Seq[R]): GraphState => (GraphState, Seq[R]) = t => {
          if (k.isEmpty) (t, coll)
          else {
            val (t1, r) = t(k.head, f(k.head, _))
            _app(k.tail, coll ++ Seq(r))(t1)
          }
        }

        _app(keys, Seq.empty)(lhs)
      }

      def getOrElse(key: String, create: GRun[Sockets])
                   (implicit b: GraphBuilder): (GraphState, Sockets) = {
        lhs.map
        .get(key)
        .map((lhs, _))
        .getOrElse({
          val (st1, scs) = create(lhs)(b)
          (st1.copy(map = st1.map ++ Map(key -> scs)), scs)
        })
      }
    }

  }
}
