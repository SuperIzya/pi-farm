package com.ilyak.pifarm.flow.configuration

import akka.stream.{Shape, SinkShape, SourceShape}

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableShape[S] {
  val node: Configuration.Node
  def build(builder: BlockBuilder): S
}

/***
  * Base trait for all plugable [[ConfigurableAutomaton]] type blocks
  */
trait ConfigurableAutomaton[S <: Shape] extends ConfigurableShape[S]

/***
  * Base trait for all plugable [[ConfigurableContainer]] type blocks
  * @tparam S - type of [[Shape]]
  */
trait ConfigurableContainer[S <: Shape] extends ConfigurableShape[S]

object ConfigurableShape {

  /** *
    * External connections of the shape
    *
    * @param inputs  : inbound connections
    * @param outputs : outbound connections
    */
  case class Connections(inputs: Map[InputId, SourceShape[_]],
                         outputs: Map[OutputId, SinkShape[_]])

  object Connections {
    def concat(first: Connections, second: Connections): Connections =
      Connections(
        first.inputs ++ second.inputs,
        first.outputs ++ second.outputs
      )
  }

}