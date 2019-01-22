package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
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
  case class ShapeConnections(inputs: List[String],
                              outputs: List[String])

  object ShapeConnections {
    val empty = ShapeConnections(List.empty, List.empty)

    implicit val connectionsMonoid = new Monoid[ShapeConnections] {
      import cats.implicits._
      override def empty: ShapeConnections = ShapeConnections.empty

      override def combine(x: ShapeConnections, y: ShapeConnections): ShapeConnections = {
        ShapeConnections(x.inputs |+| y.inputs, x.outputs |+| y.outputs)
      }
    }
  }

  /** *
    * Connections to/from the outside world external to the shape.
    *
    * @param inputs  : id -> [[Source]] for inbound connections
    * @param outputs : id -> [[Sink]] for outbound connections
    */
  case class ExternalConnections(inputs: Map[String, Source[_, _]], outputs: Map[String, Sink[_, _]])

  object ExternalConnections {
    val empty = ExternalConnections(Map.empty, Map.empty)
  }
}
