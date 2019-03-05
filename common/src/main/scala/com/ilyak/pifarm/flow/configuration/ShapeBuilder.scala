package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL}
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.ConfigurableNode._
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ContainerConnections}

import scala.language.higherKinds

/** *
  * Transforms [[ConfigurableNode]] to [[ShapeConnections]] that will be later connected to overall graph
  *
  * @tparam D implementation of [[ShapeConnections]]
  */
trait ShapeBuilder[D <: ShapeConnections] {
  /** *
    * Returns [[ShapeConnections]] with names of all connections to the outside world.
    *
    * @param conf : [[com.ilyak.pifarm.flow.configuration.Configuration.Node]] from configuration
    * @return
    */
  def build(conf: Configuration.Node)(implicit builder: GraphDSL.Builder[_]): BuildResult[D]
}

object ShapeBuilder {

  trait AutomatonBuilder extends ShapeBuilder[AutomatonConnections] {

    override def build(conf: Configuration.Node)
                      (implicit builder: GraphDSL.Builder[_]): BuildResult[AutomatonConnections]
  }

  trait ContainerBuilder extends ShapeBuilder[ContainerConnections] {

    override def build(conf: Configuration.Node)
                      (implicit builder: GraphDSL.Builder[_]): BuildResult[ContainerConnections]
  }

  trait FlowAutomatonBuilder[A <: ConfigurableAutomaton, I, O]
    extends AutomatonBuilder {
    def flow(conf: Configuration.Node): BuildResult[Flow[I, O, _]]

    def input(conf: Configuration.Node, in: SinkShape[I]): Connection.In[I]

    def output(conf: Configuration.Node, out: SourceShape[O]): Connection.Out[O]

    final override def build(conf: Configuration.Node)
                            (implicit builder: GraphDSL.Builder[_]): BuildResult[AutomatonConnections] = {
      flow(conf).map(f => {
        val shape = builder.add(f)
        val in = SinkShape(shape.in)
        val out = SourceShape(shape.out)
        AutomatonConnections(conf, Map("in" -> input(conf, in)), Map("out" -> output(conf, out)))
      })
    }
  }

}

