package com.ilyak.pifarm.flow.configuration

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Types.{ BuildResult, GBuilder }
import com.ilyak.pifarm.Units
import com.ilyak.pifarm.flow.configuration.Connection.{ ConnectShape, Sockets }
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{ AutomatonConnections, ContainerConnections }

import scala.language.{ higherKinds, implicitConversions }

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableNode[S <: ShapeConnections] {
  import com.ilyak.pifarm.State.Implicits._

  def buildShape(node: Configuration.Node): BuildResult[GBuilder[Sockets]]

  def inputs(node: Configuration.Node): Seq[Connection.In[_]]

  def outputs(node: Configuration.Node): Seq[Connection.Out[_]]

  def build(node: Configuration.Node): BuildResult[S]
  def toConnectShape(f: GBuilder[Sockets], node: Configuration.Node): ConnectShape =
    ss => _ => (ss |+| (node.id -> f), Unit)
}

object ConfigurableNode {


  /** *
    * Base trait for all plugable [[ConfigurableAutomaton]] type blocks
    */
  trait ConfigurableAutomaton extends ConfigurableNode[AutomatonConnections] {
    final override def build(node: Configuration.Node): BuildResult[AutomatonConnections] =
      buildShape(node).map(s => {
        val shape: ConnectShape = toConnectShape(s, node)
        AutomatonConnections(
          inputs(node),
          outputs(node),
          shape,
          node
        )
      })
  }

  /** *
    * Base trait for all plugable [[ConfigurableContainer]] type blocks
    *
    */
  trait ConfigurableContainer extends ConfigurableNode[ContainerConnections]


  abstract class FlowAutomaton[I: Units, O: Units] extends ConfigurableAutomaton {

    def flow(conf: Configuration.Node): BuildResult[Flow[I, O, _]]

    final override def inputs(node: Configuration.Node): Seq[Connection.In[I]] =
      Seq(Connection.In(node.inputs.head, node.id))

    final override def outputs(node: Configuration.Node): Seq[Connection.Out[O]] =
      Seq(Connection.Out(node.outputs.head, node.id))

    final override def buildShape(node: Configuration.Node): BuildResult[GBuilder[Sockets]] =
      flow(node).map(fl => b => {
        val f = b add fl
        Sockets(
          Map(node.inputs.head -> f.in),
          Map(node.outputs.head -> f.out)
        )
      })
  }

}
