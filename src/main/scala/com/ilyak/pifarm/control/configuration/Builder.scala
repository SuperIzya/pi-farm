package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream._
import cats.data.Chain
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{ConfigurableAutomaton, ConfigurableContainer}
import com.ilyak.pifarm.flow.configuration.Connection.Flw
import com.ilyak.pifarm.flow.configuration.{Configuration, Connection, ShapeConnections}
import com.ilyak.pifarm.flow.configuration.ShapeBuilder.{AutomatonBuilder, ContainerBuilder}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ExternalConnections, ExternalInputs, ExternalOutputs}
import com.ilyak.pifarm.plugins.PluginLocator

import scala.language.higherKinds

/**
  * Transforms [[Configuration.Graph]] to [[RunnableGraph]]
  */
object Builder {

  import BuilderTypes._

  def build(g: Configuration.Graph,
            connections: ExternalConnections)
           (implicit locator: PluginLocator): BuildResult[RunnableGraph[_]] = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      buildGraph(g, connections)

      ClosedShape
    })
  }

  def build(g: Configuration.Graph,
            inputs: ExternalInputs,
            outputs: ExternalOutputs)
           (implicit locator: PluginLocator): BuildResult[RunnableGraph[_]] =
    build(g, ExternalConnections(inputs, outputs))


  def buildGraph(g: Configuration.Graph, external: ExternalConnections)
                (implicit builder: Builder[_],
                 locator: PluginLocator): BuildResult[AutomatonConnections] = {

    val total = TotalConnections(g.nodes, external)
    val connections = buildInner(g.nodes, g.inners)
    connections
  }

  def buildInner(nodes: Seq[Configuration.Node],
                 inners: Map[String, Configuration.Graph])
                (implicit builder: Builder[_],
                 locator: PluginLocator): BuildResult[AutomatonConnections] = {
    type SeedType = BuildResult[Chain[AutomatonConnections]]
    nodes
      .map(n => buildNode(n, inners.get(n.id)))
      .foldLeft[SeedType](Right(Chain.empty)) {
      foldResults[Chain[AutomatonConnections], AutomatonConnections](_ append _)
    }.map { connections =>
      import cats.implicits._

      val count = connections.map(c =>
        ConnectionsCounter(
          c.inputs.keys.map(_ -> c).toMap,
          c.outputs.keys.map(_ -> c).toMap
        )
      ).foldLeft[ConnectionsMap](ConnectionsCounter.empty)((x, y) => x |+| y)

      val inputs = foldConnections[Inlet, Connection.In, Broadcast](
        "input",
        count.inputs,
        n => Broadcast(n, eagerCancel = false)
      )

      val outputs = foldConnections[Outlet, Connection.Out, Merge](
        "output",
        count.outputs,
        n => Merge(n, eagerComplete = false)
      )
    }
  }

  def buildNode(node: Configuration.Node, innerGraph: Option[Configuration.Graph])
               (implicit locator: PluginLocator,
                builder: Builder[_]): BuildResult[AutomatonConnections] =
    locator.createInstance(node.meta)
      .map {
        case b: ConfigurableAutomaton => b.build(node)
        case b: ConfigurableContainer => innerGraph.map(g => {
          buildInner(g.nodes, g.inners).flatMap {
            inner =>
              b.build(node) flatMap checkConnections(
                node,
                c => inner.inputs.connect(c.intInputs),
                c => inner.outputs.connect(c.intOutputs)
              )
          }
        }).getOrElse(BuildResult.Error(s"No inner graph for container ${node.id}"))
      }.getOrElse(BuildResult.Error(s"Failed to created instance for $node"))

  def checkConnections[S <: ShapeConnections](node: Configuration.Node,
                                              inConnected: S => BuildResult[TMap[Flw[_]]],
                                              outConnected: S => BuildResult[TMap[Flw[_]]])(conn: S): BuildResult[AutomatonConnections] =
    (inConnected(conn), outConnected(conn)) match {
      case (BuildResult.Result(ins), BuildResult.Result(outs)) => BuildResult.Result(
        AutomatonConnections(node, ins, outs)
      )
      case (false, false) => BuildResult.Error(s"Not all inputs and outputs of graph ${node.id} are connected")
      case (false, _) => BuildResult.Error(s"Not all inputs of graph ${node.id} are connected")
      case (_, false) => BuildResult.Error(s"Not all outputs of graph ${node.id} are connected")
    }
}
