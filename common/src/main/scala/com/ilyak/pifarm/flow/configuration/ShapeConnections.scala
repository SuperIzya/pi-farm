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

  private def mapConnections[C[_] <: Connection[_, _]](s: Seq[C]): Map[String, C] =
    s.map(c => c.name -> c).toMap

  implicit class ToInputs(val in: Seq[Connection.In[_]]) extends AnyVal {
    def toInputs: Inputs = mapConnections(in)
  }

  implicit class ToOutputs(val out: Seq[Connection.Out[_]]) extends AnyVal {
    def toOutputs: Outputs = mapConnections(out)
  }

  case class ExternalConnections(inputs: ExternalInputs, outputs: ExternalOutputs)

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
  case class ContainerConnections(node: Configuration.Node,
                                  inputs: Inputs,
                                  outputs: Outputs,
                                  intInputs: Outputs,
                                  intOutputs: Inputs)
    extends ShapeConnections

  case class AutomatonConnections(node: Configuration.Node,
                                  inputs: Inputs,
                                  outputs: Outputs)
    extends ShapeConnections

  trait CMap[C[_] <: Connection[_, _]] {
    def apply[S <: ShapeConnections](s: S): Map[String, C]
  }

  object CMap {
    def apply[C[_] <: Connection[_, _] : CMap]: CMap[C] = implicitly[CMap[C]]
  }

  implicit val inMap: CMap[Connection.In[_]] = new CMap[Connection.In[_]] {
    override def apply[S <: ShapeConnections](s: S): Map[String, Connection.In[_]] = s.inputs
  }
  implicit val outMap: CMap[Connection.Out[_]] = new CMap[Connection.Out[_]] {
    override def apply[S <: ShapeConnections](s: S): Map[String, Connection.Out[_]] = s.outputs
  }

  object AutomatonConnections {
    def apply(node: Configuration.Node,
              inputs: Seq[Connection.In[_]],
              outputs: Seq[Connection.Out[_]]): AutomatonConnections =
      new AutomatonConnections(node, inputs.toInputs, outputs.toOutputs)
  }
}
