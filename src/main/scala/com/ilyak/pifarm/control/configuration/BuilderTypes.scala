package com.ilyak.pifarm.control.configuration

import cats.implicits._
import cats.kernel.Semigroup

/***
  * Intermediate types used by [[Builder]]
  *
  */
private [configuration] object BuilderTypes {
  type LString = List[String]
  type PLString = (LString, LString)
  type ConnCounter = Map[String, Int]


  case class ConnectionsCounter(inputs: ConnCounter, outputs: ConnCounter)

  object ConnectionsCounter {
    def apply(inputs: List[String], outputs: List[String]): ConnectionsCounter = {
      val ltm: Seq[String] => ConnCounter = l => l.map(_ -> 1).toMap
      new ConnectionsCounter(ltm(inputs), ltm(outputs))
    }

    val empty = new ConnectionsCounter(Map.empty, Map.empty)
    implicit val cnCntrSg: Semigroup[ConnectionsCounter] = (x, y) =>
        ConnectionsCounter(x.inputs |+| y.inputs, x.outputs |+| y.outputs)
  }


  implicit class ConnCounterOps(val m: ConnCounter) extends AnyVal {
    def substract(outer: ConnCounter, ex: Map[String, _]): ConnCounter = {
      val external = ex.keys.toSeq

      def _sub(v: (String, Int)) = {
        val (key, value) = v
        val outerVal = outer.getOrElse(key, if (external.contains(key)) value else 0)
        key -> (value - outerVal)
      }

      m.map(_sub)
    }
    def filterOpen: ConnCounter = m.filter(_._2 != 0)

    def prettyPrint: String = m.toString()
  }


}
