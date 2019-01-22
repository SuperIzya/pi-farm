package com.ilyak.pifarm.flow.configuration

import akka.stream._
import cats.Monoid
import com.ilyak.pifarm.flow.configuration.ConfigurableShape.ShapeConnections

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableShape[S] {
  def getConnections(builder: BlockBuilder): (S, ShapeConnections)

  def build(builder: BlockBuilder): S
}

object ConfigurableShape {

  /** *
    * Base trait for all plugable [[ConfigurableAutomaton]] type blocks
    */
  trait ConfigurableAutomaton[S <: Shape] extends ConfigurableShape[S] {

  }

  /** *
    * Base trait for all plugable [[ConfigurableContainer]] type blocks
    *
    * @tparam S - type of [[Shape]]
    */
  trait ConfigurableContainer[S <: Shape] extends ConfigurableShape[S] {

  }

  /** *
    * Connections to/from the outside world needed by the shape.
    *
    * @param inputs  : ids of inbound connections
    * @param outputs : ids of outbound connections
    */
  case class ShapeConnections(inputs: Set[String],
                              outputs: Set[String])

  object ShapeConnections {
    val empty = ShapeConnections(Set.empty, Set.empty)

    implicit val connectionsMonoid = new Monoid[ShapeConnections] {
      override def empty: ShapeConnections = ShapeConnections.empty

      override def combine(x: ShapeConnections, y: ShapeConnections): ShapeConnections = {
        ShapeConnections(x.inputs ++ y.inputs, x.outputs ++ y.outputs)
      }
    }
  }

  /** *
    * Connections to/from the outside world external to the shape.
    *
    * @param inputs  : id -> [[SourceShape]] for inbound connections
    * @param outputs : id -> [[SinkShape]] for outbound connections
    */
  case class ExternalConnections(inputs: Map[String, SourceShape[_]], outputs: Map[String, SinkShape[_]])

  object ExternalConnections {
    val empty = ExternalConnections(Map.empty, Map.empty)

    implicit val connectionsMonoid = new Monoid[ExternalConnections] {
      override def empty: ExternalConnections = ExternalConnections.empty

      override def combine(x: ExternalConnections, y: ExternalConnections): ExternalConnections =
        ExternalConnections(x.inputs ++ y.inputs, x.outputs ++ y.outputs)
    }
  }

}