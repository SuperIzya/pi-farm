package com.ilyak.pifarm.flow.configuration

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Types.{ Result, GBuilder, SMap }
import com.ilyak.pifarm.{ Result, Units }
import com.ilyak.pifarm.flow.configuration.Connection.{ ConnectShape, External, In, Out, Sockets }
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{ AutomatonConnections, ContainerConnections, ExternalConnections }

import scala.language.{ higherKinds, implicitConversions }

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableNode[S <: ShapeConnections] {

  import com.ilyak.pifarm.State.Implicits._

  implicit def toConnectShape(v: (Configuration.Node, GBuilder[Sockets])): ConnectShape = v match {
    case (node, f) => ss => _ => (ss |+| (node.id -> f), Unit)
  }
}

object ConfigurableNode {


  /** *
    * Base trait for all plugable [[ConfigurableAutomaton]] type blocks
    */
  trait ConfigurableAutomaton extends ConfigurableNode[AutomatonConnections] {

    def inputs(node: Configuration.Node): Result[Seq[Connection.In[_]]]

    def outputs(node: Configuration.Node): Result[Seq[Connection.Out[_]]]

    def buildShape(node: Configuration.Node): Result[GBuilder[Sockets]]

    final def build(node: Configuration.Node): Result[AutomatonConnections] =
      buildShape(node).flatMap(s => {
        val shape: ConnectShape = node -> s
        Result.combine(inputs(node), outputs(node)) {
          case (in, out) => AutomatonConnections(in, out, shape, node)
        }
      })
  }


  /** *
    * Base trait for all plugable [[ConfigurableContainer]] type blocks
    *
    */
  trait ConfigurableContainer extends ConfigurableNode[ContainerConnections] {

    def inputFlows(node: Configuration.Node, inner: SMap[In[_]]): Result[(Seq[In[_]], Seq[Out[_]])]

    def outputFlows(node: Configuration.Node, inner: SMap[Out[_]]): Result[(Seq[In[_]], Seq[Out[_]])]

    def buildShape(node: Configuration.Node, inner: AutomatonConnections): Result[GBuilder[Sockets]]

    final def build(node: Configuration.Node, inner: AutomatonConnections): Result[ContainerConnections] =
      buildShape(node, inner).flatMap(s => {
        val shape: ConnectShape = node -> s
        Result.combine(inputFlows(node, inner.inputs), outputFlows(node, inner.outputs)) {
          case (inS, outS) =>
            val (ins, intIns) = inS
            val (intOuts, outs) = outS
            ContainerConnections(node, shape, ins, outs, intIns, intOuts)
        }
      })
  }


  abstract class FlowAutomaton[I: Units, O: Units] extends ConfigurableAutomaton {

    def flow(conf: Configuration.Node): Result[Flow[I, O, _]]

    override def inputs(node: Configuration.Node): Result[Seq[Connection.In[I]]] =
      Result.Res(Seq(Connection.In(node.inputs.head, node.id)))

    override def outputs(node: Configuration.Node): Result[Seq[Connection.Out[O]]] =
      Result.Res(Seq(Connection.Out(node.outputs.head, node.id)))

    final override def buildShape(node: Configuration.Node): Result[GBuilder[Sockets]] =
      flow(node).map(fl => b => {
        val f = b add fl
        Sockets(Map(node.inputs.head -> f.in), Map(node.outputs.head -> f.out))
      })
  }

}
