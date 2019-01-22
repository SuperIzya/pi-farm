package com.ilyak.pifarm.flow.configuration

import akka.stream.Shape
import ConfigurableShape.{ConfigurableAutomaton, ConfigurableContainer, ShapeConnections}
import com.ilyak.pifarm.flow.configuration.BlockBuilder.BuildResult

/***
  * Builds [[Shape]] and it's external [[ShapeConnections]]
  */
trait BlockBuilder {

  def buildAutomaton[A <: ConfigurableAutomaton[S], S <: Shape](automaton: A)
                                                               (implicit st: ShapeTransformer[A, S]): BuildResult[(S, ShapeConnections)]

  def buildContainer[C <: ConfigurableContainer[S], S <: Shape](container: C)
                                                               (implicit st: ShapeTransformer[C, S]): BuildResult[(S, ShapeConnections)]
}

object BlockBuilder {
  type BuildResult[T] = Either[String, T]
}