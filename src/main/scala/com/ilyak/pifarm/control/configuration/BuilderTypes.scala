package com.ilyak.pifarm.control.configuration

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
    def apply(inputs: List[String], outputs: List[String]) = {
      val ltm: Seq[String] => ConnCounter = l => l.map(_ -> 1).toMap
      new ConnectionsCounter(ltm(inputs), ltm(outputs))
    }

    val empty = new ConnectionsCounter(Map.empty, Map.empty)
    implicit val connCounterSemigroup = new Semigroup[ConnectionsCounter] {
      import cats.implicits._
      override def combine(x: ConnectionsCounter, y: ConnectionsCounter): ConnectionsCounter =
        ConnectionsCounter(x.inputs |+| y.inputs, x.outputs |+| y.outputs)
    }
  }
}
