package com.ilyak.pifarm.flow.configuration

import akka.stream.Shape
import akka.stream.scaladsl.GraphDSL
import ConfigurableShape.{ConfigurableAutomaton, ConfigurableContainer, ShapeConnections}

/** *
  * Transforms [[ConfigurableShape]] to [[Shape]]
  *
  * @tparam C type of configuration shape
  * @tparam S type of the result [[Shape]]
  */
trait ShapeTransformer[-C <: ConfigurableShape[S], S <: Shape] {
  def getConnections(shape: S, conf: C): ShapeConnections

  def apply(conf: C, connections: ShapeConnections, g: Configuration.Graph)
           (implicit builder: GraphDSL.Builder[_]): S
}

object ShapeTransformer {

  trait AutomatonTransformer[-A <: ConfigurableAutomaton[S], S <: Shape] extends ShapeTransformer[A, S]

  trait ContainerTransformer[-C <: ConfigurableContainer[S], S <: Shape] extends ShapeTransformer[C, S]

}