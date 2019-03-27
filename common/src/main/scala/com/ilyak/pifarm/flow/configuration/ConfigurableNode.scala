package com.ilyak.pifarm.flow.configuration

import akka.stream.scaladsl.Flow
import akka.stream.{SinkShape, SourceShape}
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.Connection.ConnectShape
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ContainerConnections}

import scala.language.{higherKinds, implicitConversions}

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableNode[S <: ShapeConnections] {
  type TShape = S

  def build(node: Configuration.Node): BuildResult[TShape]
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


  trait FlowAutomaton[I, O]
    extends ConfigurableAutomaton {
    def flow(conf: Configuration.Node): BuildResult[Flow[I, O, _]]

    def input(conf: Configuration.Node, in: SinkShape[I]): Connection.In[_]

    def output(conf: Configuration.Node, out: SourceShape[O]): Connection.Out[_]

    final override def build(conf: Configuration.Node): BuildResult[AutomatonConnections] = {
      flow(conf).map(f => {
        val in = SinkShape(f.shape.in)
        val out = SourceShape(f.shape.out)
        val c: ConnectShape = _ add f
        AutomatonConnections(
          Map("in" -> input(conf, in)),
          Map("out" -> output(conf, out)),
          c,
          conf
        )
      })
    }
  }
}
