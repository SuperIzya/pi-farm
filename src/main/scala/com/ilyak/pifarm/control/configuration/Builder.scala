package com.ilyak.pifarm.control.configuration

import com.ilyak.pifarm.flow.configuration.ConnectionHelper
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink}
import akka.stream._
import cats.data.Chain
import com.ilyak.pifarm.Build.{BuildResult, FoldResult, TMap}
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{ConfigurableAutomaton, ConfigurableContainer}
import com.ilyak.pifarm.flow.configuration.Connection.{Connected, In, Out}
import com.ilyak.pifarm.flow.configuration.{Configuration, Connection, ShapeConnections}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ContainerConnections, ExternalConnections, ExternalInputs, ExternalOutputs}
import com.ilyak.pifarm.plugins.PluginLocator
import slick.util.SQLBuilder
import slick.util.SQLBuilder.Result

import scala.language.higherKinds

/**
  * Transforms [[Configuration.Graph]] to [[RunnableGraph]]
  */
object Builder {

  import BuilderHelpers._

  def build(g: Configuration.Graph,
            connections: ExternalConnections)
           (implicit locator: PluginLocator): BuildResult[RunnableGraph[_]] =
    buildGraph(g, connections)

  def build(g: Configuration.Graph,
            inputs: ExternalInputs,
            outputs: ExternalOutputs)
           (implicit locator: PluginLocator): BuildResult[RunnableGraph[_]] =
    build(g, ExternalConnections(inputs, outputs))


  def buildGraph(g: Configuration.Graph, external: ExternalConnections)
                (implicit locator: PluginLocator): BuildResult[AutomatonConnections] = {

    val total = TotalConnections(g.nodes, external)
    val connections = buildInner(g.nodes, g.inners)
    connections
  }

  def buildInner(nodes: Seq[Configuration.Node],
                 inners: Map[String, Configuration.Graph])
                (implicit locator: PluginLocator): BuildResult[AutomatonConnections] = {
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

      val inputs = foldConnections[Inlet, Connection.In](
        "input",
        count.inputs,
        _.map(i => GraphDSL.create() { _ => SinkShape(i)})
          .foldLeft(Flow[_]) { _ alsoTo _ }
      )

      val outputs = foldConnections[Outlet, Connection.Out](
        "output",
        count.outputs,
        _.map(o => GraphDSL.create() { _ => SourceShape(o)})
          .foldLeft(Flow[_]) { _ merge _ }
      )
    }
  }

  def buildNode(node: Configuration.Node, innerGraph: Option[Configuration.Graph])
               (implicit locator: PluginLocator): BuildResult[AutomatonConnections] =
    locator.createInstance(node.meta)
      .map {
        case b: ConfigurableAutomaton => b.build(node)
        case b: ConfigurableContainer => innerGraph.map(g => {
          buildInner(g.nodes, g.inners).flatMap {
            inner =>
              b.build(node) flatMap connectInner(
                node,
                c => inner.inputs.connect(c.intInputs),
                c => inner.outputs.connect(c.intOutputs)
              )
          }
        }).getOrElse(BuildResult.Error(s"No inner graph for container ${node.id}"))
      }.getOrElse(BuildResult.Error(s"Failed to created instance for $node"))

  def connectInner(node: Configuration.Node,
                   connectIn: ContainerConnections => FoldResult[Closed[In]],
                   connectOut: ContainerConnections => FoldResult[Closed[Out]])
                  (conn: ContainerConnections): BuildResult[AutomatonConnections] =
    (connectIn(conn), connectOut(conn)) match {
      case (BuildResult.Result(ins), BuildResult.Result(outs)) =>
        def filterOpened[T[_]](f: TMap[Closed[T]]): TMap[T[_]] = f.collect{ case (k: String, Right(value)) => k -> value}

        BuildResult.Result(
          AutomatonConnections(node, filterOpened(ins), filterOpened(outs))
        )
      case (BuildResult.Error(e1), BuildResult.Error(e2)) => BuildResult.Error(
        s"""
           |$e1
           |$e2
         """.stripMargin
      )
      case (BuildResult.Error(e), _) => BuildResult.Error(e)
      case (_, BuildResult.Error(e)) => BuildResult.Error(e)
    }
}
