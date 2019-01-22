package com.ilyak.pifarm.control.configuration

import akka.stream.{ClosedShape, Shape}
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import com.ilyak.pifarm.flow.Messages.{AggregateCommand, SensorData}
import com.ilyak.pifarm.flow.configuration.BlockBuilder.BuildResult
import com.ilyak.pifarm.flow.configuration.ConfigurableShape._
import com.ilyak.pifarm.flow.configuration.Configuration.Node
import com.ilyak.pifarm.flow.configuration._
import com.ilyak.pifarm.plugins.PluginLocator

import scala.util.{Failure, Try}

/**
  * Transforms [[Configuration.Graph]] to [[RunnableGraph]]
  */
object Builder {

  import BlockBuilder.BuildResult
  import BuilderTypes._
  import cats.implicits._

  def build(g: Configuration.Graph,
            inputs: Map[String, Source[SensorData[_], _]],
            outputs: Map[String, Sink[AggregateCommand, _]])
           (implicit locator: PluginLocator): RunnableGraph[_] =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      val inlets = inputs.map(p => p._1 -> builder.add(p._2))
      val outlets = outputs.map(p => p._1 -> builder.add(p._2))
      compileConfiguration(g, ExternalConnections(inlets, outlets))

      ClosedShape
    })

  def compileConfiguration(g: Configuration.Graph, external: ExternalConnections)
                          (implicit builder: Builder[_],
                           locator: PluginLocator): BuildResult[Shape] = {
    val empty = ConnectionsCounter.empty

    val connectionsCount = g.nodes.map(n => ConnectionsCounter(n.inputs, n.outputs))
      .foldLeft(empty)(_ |+| _)



    val compiledNodes = g.nodes
      .map(n => compileNode(n, g.inners.get(n.id)))
  }

  def compileNode(node: Configuration.Node, inner: Option[Configuration.Graph])
                 (implicit locator: PluginLocator,
                  builder: Builder[_]): BuildResult[(ConfigurableShape[_], ShapeConnections)] =
    locator.createInstance(node.meta)
      .map(block => Right(block -> block.getConnections(new Helper(node, inner))))
      .getOrElse(Left(s"Failed to created instance for $node"))


  private class Helper(node: Configuration.Node,
                       g: Option[Configuration.Graph])
                      (implicit builder: GraphDSL.Builder[_]) extends BlockBuilder {
    def buildAutomaton[A <: ConfigurableAutomaton[S], S <: Shape](automaton: A)
                                                                 (implicit st: ShapeTransformer[A, S]): BuildResult[(S, ShapeConnections)] = {
      val shape = st(automaton)
      val connections = st.getConnections(shape, automaton)
      Right(shape -> connections)
    }

    def buildContainer[C <: ConfigurableContainer[S], S <: Shape](container: C)
                                                                 (implicit st: ShapeTransformer[C, S]): BuildResult[S] = {
      g.map(_.inners.get(node.id)) match {
        case None => Left(s"Container ${node.id} has no inner graph")
        case Some(inner) =>
          val shape = st()
      }
    }
  }

}