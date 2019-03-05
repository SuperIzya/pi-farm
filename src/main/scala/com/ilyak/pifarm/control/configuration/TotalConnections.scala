package com.ilyak.pifarm.control.configuration

import cats.data.Chain
import cats.implicits._
import com.ilyak.pifarm.flow.configuration.Configuration
import BuilderTypes._
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.control.configuration.Builder.BuildResult
import com.ilyak.pifarm.control.configuration.TotalConnections.SeedType
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ExternalConnections}

/** *
  * Used by [[Builder]] to count total connections in [[Configuration.Graph]]
  *
  * @param connCounter : Total of all connections originated in [[Configuration.Graph]]
  * @param external    : external to this [[Configuration.Graph]] connections
  */
class TotalConnections private(val connCounter: ConnectionsCounter[Int], external: ExternalConnections) {

  val inputConnSumMap = connCounter.inputs.substract(connCounter.outputs, external.outputs)
  val isInputConnected = inputConnSumMap
    .values
    .forall(_ == 0)

  val outputConnSumMap = connCounter.outputs.substract(connCounter.inputs, external.inputs)
  val isOutputConnected = outputConnSumMap
    .values
    .forall(_ == 0)

  def seed: SeedType = {
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
  type SeedType = BuildResult[Chain[AutomatonConnections]]

  def apply(nodes: Seq[Configuration.Node], external: ExternalConnections): TotalConnections = {
    val empty = ConnectionsCounter.empty[Int]

    val connCounter = nodes.map(n => ConnectionsCounter(n.inputs, n.outputs))
      .foldLeft(empty)(_ |+| _)

    new TotalConnections(connCounter, external)
  }
}