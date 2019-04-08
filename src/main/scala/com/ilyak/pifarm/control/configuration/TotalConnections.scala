package com.ilyak.pifarm.control.configuration

import cats.data.Chain
import cats.implicits._
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ExternalConnections}



/** *
  * Used by [[Builder]] to count total connections in [[Configuration.Graph]]
  *
  * @param connCounter : Total of all connections originated in [[Configuration.Graph]]
  * @param external    : external to this [[Configuration.Graph]] connections
  */
class TotalConnections private(val connCounter: ConnectionsCounter[Int], external: ExternalConnections) {
  import TotalConnections._

  val inputConnSumMap: SMap[Int] = connCounter.inputs.substract(connCounter.outputs, external.outputs)
  val isInputConnected: Boolean = inputConnSumMap
    .values
    .forall(_ == 0)

  val outputConnSumMap: SMap[Int] = connCounter.outputs.substract(connCounter.inputs, external.inputs)
  val isOutputConnected: Boolean = outputConnSumMap
    .values
    .forall(_ == 0)

  def seed: BuildResult[Chain[AutomatonConnections]] = {
    if (!isInputConnected && !isOutputConnected) {
      val ins = inputConnSumMap.filterOpen
      val outs = outputConnSumMap.filterOpen
      Left(
        s"""
           |Input connections are not properly matched with output connections:
           |ins: ${ins.prettyPrint}
           |outs: ${outs.prettyPrint}
         """.stripMargin
      )
    }
    else if (!isInputConnected) {
      val ins = inputConnSumMap.filterOpen
      Left(
        s"""
           |Input connections are not properly matched with output connections:
           |ins: ${ins.prettyPrint}
         """.stripMargin
      )
    }
    else if (!isOutputConnected) {
      val outs = outputConnSumMap.filterOpen
      Left(
        s"""
           |Input connections are not properly matched with output connections:
           |outs: ${outs.prettyPrint}
         """.stripMargin
      )
    }
    else Right(Chain.empty)
  }

}

private [configuration] object TotalConnections {

  implicit class ConnectionsCounterOps(val m: SMap[Int]) extends AnyVal {
    def substract(outer: SMap[Int], ex: SMap[_]): SMap[Int] = {
      val external = ex.keys.toSeq

      def _sub(v: (String, Int)) = {
        val (key, value) = v
        val outerVal = outer.getOrElse(key, if (external.contains(key)) value else 0)
        key -> (value - outerVal)
      }

      m.map(_sub)
    }

    def filterOpen: SMap[Int] = m.filter(_._2 != 0)

    def prettyPrint: String = m.toString()
  }
  def apply(nodes: Seq[Configuration.Node], external: ExternalConnections): TotalConnections = {
    val empty = ConnectionsCounter.empty[Int]

    val connCounter = nodes.map(n => ConnectionsCounter(n.inputs, n.outputs))
      .foldLeft(empty)(_ |+| _)

    new TotalConnections(connCounter, external)
  }
}