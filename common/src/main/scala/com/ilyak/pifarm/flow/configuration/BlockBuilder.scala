package com.ilyak.pifarm.flow.configuration

import akka.stream.Shape
import com.ilyak.pifarm.flow.configuration.ConfigurableShape.Connections

trait BlockBuilder {
  case class BuildResult[S](shape: S, connections: Connections)

  def buildAutomaton[A <: ConfigurableAutomaton[S], S <: Shape](automaton: A)
                                                               (implicit st: ShapeTransformer[A, S]): BuildResult[S]

  def buildContainer[C <: ConfigurableContainer[S], S <: Shape](container: C)
                                                               (implicit st: ShapeTransformer[C, S]): BuildResult[S]
}
