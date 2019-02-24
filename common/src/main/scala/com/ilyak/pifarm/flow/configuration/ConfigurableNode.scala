package com.ilyak.pifarm.flow.configuration

import akka.stream.scaladsl.GraphDSL
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ContainerConnections}

import scala.language.{higherKinds, implicitConversions}

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableNode[S <: ShapeConnections] {

  def build(node: Configuration.Node)(implicit builder: GraphDSL.Builder[_]): S
}

object ConfigurableNode {

  /** *
    * Base trait for all plugable [[ConfigurableAutomaton]] type blocks
    */
  trait ConfigurableAutomaton extends ConfigurableNode[AutomatonConnections]

  /** *
    * Base trait for all plugable [[ConfigurableContainer]] type blocks
    *
    */
  trait ConfigurableContainer extends ConfigurableNode[ContainerConnections]

}
