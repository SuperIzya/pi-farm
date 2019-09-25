package com.ilyak.pifarm.flow.configuration

import akka.stream.Shape
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.flow.configuration.Connection.{ ConnectShape, External, TConnection }

import scala.language.higherKinds

/** *
  * Connection of the [[Shape]] built by [[ConfigurableNode]]
  *
  */
sealed trait ShapeConnections {

  import ShapeConnections.{ Inputs, Outputs }

  val node: Option[Configuration.Node]
  val inputs: Inputs
  val outputs: Outputs
  val shape: ConnectShape
}

object ShapeConnections {

  import ConnectionHelper._

  type Inputs = SMap[Connection.In[_]]
  type Outputs = SMap[Connection.Out[_]]

  type ExternalInputs = SMap[External.ExtIn[_]]
  type ExternalOutputs = SMap[External.ExtOut[_]]

  case class ExternalConnections private(
    inputs: ExternalInputs,
    outputs: ExternalOutputs
  )

  object ExternalConnections {
    def apply(inputs: Seq[External.ExtIn[_]], outputs: Seq[External.ExtOut[_]]): ExternalConnections =
      new ExternalConnections(inputs.toExtInputs, outputs.toExtOutputs)
  }

  /** *
    *
    * All connections of container-node.
    *
    * @param node       : Configuration node.
    * @param inputs     : [Inputs] for collecting incoming external traffic
    * @param outputs    : [Outputs] for providing externally outgoing traffic
    * @param intInputs  : [Outputs] sources for internal [[Shape]]'s inputs
    * @param intOutputs : [Inputs] sinks for internal [[Shape]]'s outputs
    */
  case class ContainerConnections private(
    node: Option[Configuration.Node],
    inputs: Inputs,
    outputs: Outputs,
    intInputs: Outputs,
    intOutputs: Inputs,
    shape: ConnectShape
  ) extends ShapeConnections

  object ContainerConnections {
    def apply(node: Configuration.Node,
              shape: ConnectShape,
              inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]],
              intInputs: Seq[Connection.Out[_]],
              intOutputs: Seq[Connection.In[_]]): ContainerConnections =
      new ContainerConnections(
                                Some(node),
                                inputs.toInputs,
                                outputs.toOutputs,
                                intInputs.toIntInputs,
                                intOutputs.toIntOutputs,
                                shape
                              )
  }

  case class AutomatonConnections private(
    node: Option[Configuration.Node],
    inputs: Inputs,
    outputs: Outputs,
    shape: ConnectShape
  ) extends ShapeConnections

  // TODO: Move all apply methods to trait
  object AutomatonConnections {
    def apply(inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]],
              shape: ConnectShape,
              node: Configuration.Node): AutomatonConnections =
      apply(inputs.toInputs, outputs.toOutputs, shape, node)

    def apply(inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]],
              shape: ConnectShape): AutomatonConnections =
      apply(inputs.toInputs, outputs.toOutputs, shape)

    def apply(inputs: SMap[Connection.In[_]],
              outputs: SMap[Connection.Out[_]],
              shape: ConnectShape,
              node: Configuration.Node): AutomatonConnections =
      new AutomatonConnections(Some(node), inputs, outputs, shape)

    def apply(inputs: SMap[Connection.In[_]],
              outputs: SMap[Connection.Out[_]],
              shape: ConnectShape): AutomatonConnections =
      new AutomatonConnections(None, inputs, outputs, shape)
  }

  trait ShapesConnections[C[_] <: TConnection] {
    def get[S <: ShapeConnections](s: S): SMap[C[_]]
  }

  object ShapesConnections {
    def apply[C[_] <: TConnection : ShapesConnections]: ShapesConnections[C] =
      implicitly[ShapesConnections[C]]
  }

  implicit val inMap: ShapesConnections[Connection.In] = new ShapesConnections[Connection.In] {
    override def get[S <: ShapeConnections](s: S): SMap[Connection.In[_]] = s.inputs
  }

  implicit val outMap: ShapesConnections[Connection.Out] = new ShapesConnections[Connection.Out] {
    override def get[S <: ShapeConnections](s: S): SMap[Connection.Out[_]] = s.outputs
  }
}
