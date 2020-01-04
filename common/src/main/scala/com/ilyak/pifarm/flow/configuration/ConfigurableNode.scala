package com.ilyak.pifarm.flow.configuration

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Units
import com.ilyak.pifarm.flow.configuration.Configuration.ParseMeta
import com.ilyak.pifarm.flow.configuration.Connection.{In, Out, Sockets}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{
  AutomatonConnections,
  ContainerConnections
}
import com.ilyak.pifarm.types._

import scala.language.{higherKinds, implicitConversions}

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableNode[S <: ShapeConnections] {
//
//  implicit def toConnectState(
//    v: (Configuration.Node, GBuilder[Sockets])
//  ): ConnectState = v match {
//    case (node, f) =>
//      ConnectState { ss => _ =>
//        (ss.add(node.id, f), ())
//      }
//  }
}

object ConfigurableNode {

  case class XLet private (name: String, unit: String)

  object XLet {
    def apply[T: Units](name: String): XLet = XLet(name, Units[T].name)
  }

  trait NodeCompanion[T <: ConfigurableNode[_]] {
    val inputs: List[XLet]
    val outputs: List[XLet]
    val blockType: BlockType
    val name: String
    val creator: ParseMeta[T]

    private def namesOf(lets: List[XLet]): List[String] = lets.map(_.name)

    def inputNames: List[String] = namesOf(inputs)

    def outputNames: List[String] = namesOf(outputs)
  }

  object NodeCompanion {
    def apply[T <: ConfigurableNode[_]: NodeCompanion]: NodeCompanion[T] =
      implicitly[NodeCompanion[T]]
  }

  /** *
    * Base trait for all plugable [[ConfigurableAutomaton]] type blocks
    */
  trait ConfigurableAutomaton extends ConfigurableNode[AutomatonConnections] {

    def inputs(node: Configuration.Node): Result[Seq[Connection.In[_]]]

    def outputs(node: Configuration.Node): Result[Seq[Connection.Out[_]]]

    def buildShape(node: Configuration.Node): Result[GBuilder[Sockets]]

    final def build(node: Configuration.Node): Result[AutomatonConnections] =
      buildShape(node).flatMap(s => {
        val shape: ConnectState = GraphState.add(node.id, GState.pure(s))
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

    def inputFlows(node: Configuration.Node,
                   inner: SMap[In[_]]): Result[(Seq[In[_]], Seq[Out[_]])]

    def outputFlows(node: Configuration.Node,
                    inner: SMap[Out[_]]): Result[(Seq[In[_]], Seq[Out[_]])]

    def buildShape(node: Configuration.Node,
                   inner: AutomatonConnections): Result[GBuilder[Sockets]]

    final def build(node: Configuration.Node,
                    inner: AutomatonConnections): Result[ContainerConnections] =
      buildShape(node, inner).flatMap(s => {
        val shape: ConnectState = GraphState.add(node.id, GState.pure(s))
        Result.combine(
          inputFlows(node, inner.inputs),
          outputFlows(node, inner.outputs)
        ) {
          case (inS, outS) =>
            val (ins, intIns) = inS
            val (intOuts, outs) = outS
            ContainerConnections(node, shape, ins, outs, intIns, intOuts)
        }
      })
  }

  abstract class FlowAutomaton[I: Units, O: Units]
      extends ConfigurableAutomaton {

    def flow(conf: Configuration.Node): Result[Flow[I, O, _]]

    override def inputs(
      node: Configuration.Node
    ): Result[Seq[Connection.In[I]]] =
      Result.Res(Seq(Connection.In(node.inputs.head, node.id)))

    override def outputs(
      node: Configuration.Node
    ): Result[Seq[Connection.Out[O]]] =
      Result.Res(Seq(Connection.Out(node.outputs.head, node.id)))

    final override def buildShape(
      node: Configuration.Node
    ): Result[GBuilder[Sockets]] =
      flow(node).map(
        fl =>
          b => {
            val f = b add fl
            Sockets(
              Map(node.inputs.head -> f.in),
              Map(node.outputs.head -> f.out)
            )
        }
      )
  }

  object FlowAutomaton {
    def getCompanion[I: Units, O: Units, T <: ConfigurableNode[_]](
      _name: String,
      _creator: ParseMeta[T]
    ): NodeCompanion[T] =
      new NodeCompanion[T] {
        override val inputs: List[XLet] =
          List(XLet[I]("in"))
        override val outputs: List[XLet] =
          List(XLet[O]("out"))
        override val blockType: BlockType = BlockType.Automaton
        override val name: String = _name
        override val creator: ParseMeta[T] = _creator
      }
  }

}
