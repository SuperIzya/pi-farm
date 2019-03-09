package com.ilyak.pifarm.flow.configuration

import akka.stream.Shape
import com.ilyak.pifarm.flow.configuration.Connection.External

import scala.language.higherKinds

/** *
  * Connection of the [[Shape]] built by [[ConfigurableNode]] but not connected to overall [[akka.stream.Graph]]
  *
  */
sealed trait ShapeConnections {

  import ShapeConnections.{Inputs, Outputs}

  val node: Configuration.Node
  val inputs: Inputs
  val outputs: Outputs
}

object ShapeConnections {
  type Inputs = Map[String, Connection.In[_]]
  type Outputs = Map[String, Connection.Out[_]]

  type ExternalInputs = Map[String, External.In[_]]
  type ExternalOutputs = Map[String, External.Out[_]]

  private def mapConnections[C[_] <: TConnection[_]](s: Seq[C[_]]): Map[String, C[_]] =
    s.map(c => c.name -> c).toMap

  implicit class ToInputs(val in: Seq[Connection.In[_]]) extends AnyVal {
    def toInputs: Inputs = mapConnections(in)
  }

  implicit class ToOutputs(val out: Seq[Connection.Out[_]]) extends AnyVal {
    def toOutputs: Outputs = mapConnections(out)
  }

  implicit class ToIntInputs(val in: Seq[Connection.Out[_]]) extends AnyVal {
    def toIntInputs: Outputs = mapConnections(in)
  }

  implicit class ToIntOutputs(val in: Seq[Connection.In[_]]) extends AnyVal {
    def toIntOutputs: Inputs = mapConnections(in)
  }

  implicit class ToExtInputs(val in: Seq[External.In[_]]) extends AnyVal {
    def toExtInputs: ExternalInputs = mapConnections(in)
  }

  implicit class ToExtOutputs(val out: Seq[External.Out[_]]) extends AnyVal {
    def toExtOutputs: ExternalOutputs = mapConnections(out)
  }

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
  case class ContainerConnections private(node: Configuration.Node,
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
        node,
        inputs.toInputs,
        outputs.toOutputs,
        intInputs.toIntInputs,
        intOutputs.toIntOutputs
      )
  }


  case class AutomatonConnections private(node: Configuration.Node,
                                          inputs: Inputs,
                                          outputs: Outputs)
    extends ShapeConnections


  object AutomatonConnections {
    def apply(node: Configuration.Node,
              inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]]): AutomatonConnections =
      new AutomatonConnections(node, inputs.toInputs, outputs.toOutputs)
  }


  trait CMap[C[_] <: TConnection[_]] {
    def apply[S <: ShapeConnections](s: S): Map[String, C[_]]
  }

  object CMap {
    def apply[C[_] <: TConnection[_] : CMap]: CMap[C] = implicitly[CMap[C]]
  }

  implicit val inMap: CMap[Connection.In] = new CMap[Connection.In] {
    override def apply[S <: ShapeConnections](s: S): Map[String, Connection.In[_]] = s.inputs
  }
  implicit val outMap: CMap[Connection.Out] = new CMap[Connection.Out] {
    override def apply[S <: ShapeConnections](s: S): Map[String, Connection.Out[_]] = s.outputs
  }
}
