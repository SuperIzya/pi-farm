package com.ilyak.pifarm.control.configuration

import akka.stream.{ClosedShape, Shape}
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import com.ilyak.pifarm.flow.Messages.{AggregateCommand, SensorData}
import com.ilyak.pifarm.flow.configuration.ConfigurableShape.Connections
import com.ilyak.pifarm.flow.configuration._
import com.ilyak.pifarm.plugins.PluginLocator

import scala.util.{Failure, Try}

/**
  * Transforms [[Configuration.Graph]] to [[RunnableGraph]]
  */
object Compiler  {

  def compile(g: Configuration.Graph,
              inputs: Map[InputId, Source[SensorData[_], _]],
              outputs: Map[OutputId, Sink[AggregateCommand, _]])
             (implicit locator: PluginLocator): RunnableGraph[_] =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      val inlets = inputs.map(p => p._1 -> builder.add(p._2))
      val outlets = outputs.map(p => p._1 -> builder.add(p._2))
      compileConfiguration(g, Connections(inlets, outlets))

      ClosedShape
    })

  def compileConfiguration(g: Configuration.Graph, external: Connections)
                          (implicit builder: Builder[_],
                           locator: PluginLocator): Option[Shape] = {
    val allConnections = g.nodes
      .map(n => compileNode(n, g.inners.get(n.id)))
      .foldLeft(external)((a, v) => v.map(Connections.concat(a, _)).getOrElse(a))

  }

  def compileNode(node: Configuration.Node, inner: Option[Configuration.Graph])
                 (implicit locator: PluginLocator,
                  builder: Builder[_]): Option[Connections] = {
    locator.createInstance(node.meta) match {
      case None => None
      case Some(block) => block.build()
    }

  }
  private class Helper(g: Configuration.Graph)
                      (implicit builder: GraphDSL.Builder[_]) extends BlockBuilder {
    def buildAutomaton[A <: ConfigurableAutomaton[S], S <: Shape](automaton: A)
                                                                 (implicit st: ShapeTransformer[A, S]): BuildResult[S] = {
      val shape = st(automaton)
      val connections = st.getConnections(shape, automaton)
      BuildResult(shape, connections)
    }

    def buildContainer[C <: ConfigurableContainer[S], S <: Shape](container: C)
                                                                 (implicit st: ShapeTransformer[C, S]): Try[BuildResult[S]] = {
      g.inners.get(container.node.id) match {
        case None => Failure(new Exception(s"Container ${container.node.id} has no inner graph"))
        case Some(inner) =>
          val shape = st()
      }
    }
  }
}