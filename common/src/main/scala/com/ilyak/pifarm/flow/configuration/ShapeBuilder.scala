package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL}
import com.ilyak.pifarm.flow.Messages.Data
import com.ilyak.pifarm.flow.configuration.ConfigurableNode._
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ContainerConnections}

import scala.language.higherKinds

/** *
  * Transforms [[ConfigurableNode]] to [[Shape]]
  *
  * @tparam C type of configuration shape
  * @tparam S type of the result [[Shape]]
  */
trait ShapeBuilder[D <: ShapeConnections] {
  /** *
    * Returns [[ShapeConnections]] with names of all connections to the outside world.
    *
    * @param conf : [[com.ilyak.pifarm.flow.configuration.Configuration.Node]] from configuration
    * @return
    */
  def build(conf: Configuration.Node)(implicit builder: GraphDSL.Builder[_]): D
}

object ShapeBuilder {

  trait AutomatonBuilder extends ShapeBuilder[AutomatonConnections] {

    override def build(conf: Configuration.Node)
                      (implicit builder: GraphDSL.Builder[_]): AutomatonConnections
  }

  trait ContainerBuilder extends ShapeBuilder[ContainerConnections] {

    override def build(conf: Configuration.Node)
                      (implicit builder: GraphDSL.Builder[_]): ContainerConnections
  }

  trait FlowAutomatonBuilder[A <: ConfigurableAutomaton, I <: Data, O <: Data]
    extends AutomatonBuilder {
    def flow: Flow[I, O, _]

    override def build(conf: Configuration.Node)
                      (implicit builder: GraphDSL.Builder[_]): AutomatonConnections = {
      val shape = builder.add(flow)
      val in = SinkShape(shape.in)
      val out = SourceShape(shape.out)
      AutomatonConnections(conf, Map("in" -> in), Map("out" -> out))
    }
  }

}

