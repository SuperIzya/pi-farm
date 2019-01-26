package com.ilyak.pifarm.flow.configuration

import akka.stream.Shape
import ConfigurableShape.{ConfigurableAutomaton, ConfigurableContainer, ShapeConnections}
import com.ilyak.pifarm.flow.configuration.BlockBuilder.{BuildResult, BuiltNode}
import com.ilyak.pifarm.flow.configuration.ShapeTransformer.{AutomatonTransformer, ContainerTransformer}

/** *
  * Builds [[Shape]] and it's external [[ShapeConnections]]
  */
trait BlockBuilder {

  def buildAutomaton[A[_] <: ConfigurableAutomaton[_], S <: Shape]
  (automaton: A[S])(implicit st: AutomatonTransformer[A[S], S]): BuildResult[BuiltNode]

  def buildContainer[C[_] <: ConfigurableContainer[_], S <: Shape]
  (container: C[S])(implicit st: ContainerTransformer[C[S], S]): BuildResult[BuiltNode]
}

object BlockBuilder {
  type BuildResult[+T] = Either[String, T]
  type CompiledGraph = BuildResult[List[BuiltNode]]

  case class BuiltNode(node: Configuration.Node,
                       shapeObject: ConfigurableShape[_]#ShapeObject)

}
