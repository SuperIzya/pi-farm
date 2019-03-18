package com.ilyak.pifarm.flow.configuration

import akka.stream.Shape
import com.ilyak.pifarm.Build.TMap
import com.ilyak.pifarm.flow.configuration.Connection.{Connect, External, TConnection}

import scala.language.higherKinds

/** *
  * Connection of the [[Shape]] built by [[ConfigurableNode]] but not connected to overall [[akka.stream.Graph]]
  *
  */
sealed trait ShapeConnections {

  import ShapeConnections.{Inputs, Outputs}

  val node: Option[Configuration.Node]
  val inputs: Inputs
  val outputs: Outputs
}

object ShapeConnections {
  import ConnectionHelper._

  type Inputs = TMap[Connection.In[_]]
  type Outputs = TMap[Connection.Out[_]]

  type ExternalInputs = TMap[External.In[_]]
  type ExternalOutputs = TMap[External.Out[_]]


  case class ExternalConnections private(inputs: ExternalInputs, outputs: ExternalOutputs)

  object ExternalConnections {
    def apply(inputs: Seq[External.In[_]], outputs: Seq[External.Out[_]]): ExternalConnections =
      new ExternalConnections(
        inputs.toExtInputs,
        outputs.toExtOutputs
      )

  }

  /** *
    *
    * Connections of container for other [[Shape]].
    *
    * @param node       : Configuration node.
    * @param inputs     : [[Map[String, Sink[Data, _] ] ]] for collecting incoming external traffic
    * @param outputs    : [[Map[String, Source[Data, _] ] ]] for providing externally outgoing traffic
    * @param intInputs  : [[Map[String, Source[Data, _] ] ]] sources for internal [[Shape]]'s inputs
    * @param intOutputs : [[Map[String, Sink[Data, _] ] ]] sinks for internal [[Shape]]'s outputs
    */
  case class ContainerConnections private(node: Option[Configuration.Node],
                                          inputs: Inputs,
                                          outputs: Outputs,
                                          intInputs: Outputs,
                                          intOutputs: Inputs)
    extends ShapeConnections

  object ContainerConnections {
    def apply(node: Configuration.Node,
              inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]],
              intInputs: Seq[Connection.Out[_]],
              intOutputs: Seq[Connection.In[_]]): ContainerConnections =
      new ContainerConnections(
        Some(node),
        inputs.toInputs,
        outputs.toOutputs,
        intInputs.toIntInputs,
        intOutputs.toIntOutputs
      )
  }


  case class AutomatonConnections private(node: Option[Configuration.Node],
                                          inputs: Inputs,
                                          outputs: Outputs,
                                          closed: Connect)
    extends ShapeConnections


  // TODO: Move all apply methods to trait
  object AutomatonConnections {
    def apply(inputs: TMap[Connection.In[_]],
              outputs: TMap[Connection.Out[_]],
              node: Configuration.Node): AutomatonConnections =
      apply(inputs, outputs, Connect.empty, node)
    def apply(inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]],
              node: Configuration.Node): AutomatonConnections =
      apply(inputs, outputs, node)
    def apply(inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]]): AutomatonConnections =
      apply(inputs, outputs, Connect.empty)

    def apply(inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]],
              closed: Connect,
              node: Configuration.Node): AutomatonConnections =
      apply(inputs, outputs, closed, node)
    def apply(inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]],
              closed: Connect): AutomatonConnections =
      apply(inputs.toInputs, outputs.toOutputs, closed)

    def apply(inputs: TMap[Connection.In[_]],
              outputs: TMap[Connection.Out[_]],
              closed: Connect,
              node: Configuration.Node): AutomatonConnections =
      new AutomatonConnections(Some(node), inputs, outputs, closed)
    def apply(inputs: TMap[Connection.In[_]],
              outputs: TMap[Connection.Out[_]],
              closed: Connect): AutomatonConnections =
      new AutomatonConnections(None, inputs, outputs, closed)
  }


  trait CMap[C[_] <: TConnection[_]] {
    def apply[S <: ShapeConnections](s: S): TMap[C[_]]
  }

  object CMap {
    def apply[C[_] <: TConnection[_] : CMap]: CMap[C] = implicitly[CMap[C]]
  }

  implicit val inMap: CMap[Connection.In] = new CMap[Connection.In] {
    override def apply[S <: ShapeConnections](s: S): TMap[Connection.In[_]] = s.inputs
  }
  implicit val outMap: CMap[Connection.Out] = new CMap[Connection.Out] {
    override def apply[S <: ShapeConnections](s: S): TMap[Connection.Out[_]] = s.outputs
  }
}
