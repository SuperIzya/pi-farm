package com.ilyak.pifarm.flow.configuration

import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import akka.stream.{Graph, Shape, SinkShape, SourceShape}
import com.ilyak.pifarm.flow.Messages.Data

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
  type InputSocket[T <: Data] = SinkShape[T]
  type OutputSocket[T <: Data] = SourceShape[T]
  type Inputs = Map[String, InputSocket[_ <: Data]]
  type Outputs = Map[String, OutputSocket[_ <: Data]]

  type ExternalInput[T <: Data] = Source[T, _]
  type ExternalOutput[T <: Data] = Sink[T, _]
  type ExtInputs = Map[String, ExternalInput[_ <: Data]]
  type ExtOutputs = Map[String, ExternalOutput[_ <: Data]]

  /** *
    * Connections to the outside world of this [[Shape]].
    *
    * @param inputs  : input connections
    * @param outputs : output connections
    */
  case class ExternalConnections(inputs: ExtInputs, outputs: ExtOutputs)

  object ExternalConnections {
    def apply(cc: ContainerConnections): ExternalConnections = {
      def toGraph[T <: Shape](v: T): Graph[T, _] = GraphDSL.create() { _ => v }

      new ExternalConnections(
        cc.intInputs.map {
          case (k: String, v: OutputSocket[_]) => k -> Source.fromGraph(toGraph(v))
        },
        cc.intOutputs.map {
          case (k: String, v: InputSocket[_]) => k -> Sink.fromGraph(toGraph(v))
        }
      )
    }
  }

  /** *
    *
    * Connections of container for other [[Shape]].
    *
    * @param node       : Configuration node.
    * @param inputs     : [[Map[String, Sink[Data, _] ] ]] for collecting incoming external traffic
    * @param outputs    : [[Map[String, Source[Data, _] ] ]] for providing externally outgoing traffic
    * @param intInputs  : [[Map[String, Source[Data, _] ] ]] sources for inputs of internal [[Shape]]
    * @param intOutputs : [[Map[String, Sink[Data, _] ] ]] sinks for outputs of internal [[Shape]]
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

}