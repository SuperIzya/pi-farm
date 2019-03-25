package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.ConfigurableNode._
import com.ilyak.pifarm.flow.configuration.Connection.Connect
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
  def build(conf: Configuration.Node): BuildResult[D]
}

object ShapeBuilder {

  trait AutomatonBuilder extends ShapeBuilder[AutomatonConnections] {

    override def build(conf: Configuration.Node): BuildResult[AutomatonConnections]
  }

  trait ContainerBuilder extends ShapeBuilder[ContainerConnections] {

    override def build(conf: Configuration.Node): BuildResult[ContainerConnections]
  }

  trait FlowAutomatonBuilder[A <: ConfigurableAutomaton, I, O]
    extends AutomatonBuilder {
    def flow(conf: Configuration.Node): BuildResult[Flow[I, O, _]]

    def input(conf: Configuration.Node, in: SinkShape[I]): Connection.In[_]

    def output(conf: Configuration.Node, out: SourceShape[O]): Connection.Out[_]

    final override def build(conf: Configuration.Node): BuildResult[AutomatonConnections] = {
      flow(conf).map(f => {
        val in = SinkShape(f.shape.in)
        val out = SourceShape(f.shape.out)
        val c: Connect = _ add f
        AutomatonConnections(
          Map("in" -> input(conf, in)),
          Map("out" -> output(conf, out)),
          c,
          conf
        )
      })
    }
  }

}

