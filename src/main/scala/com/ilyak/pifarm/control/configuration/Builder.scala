package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge, RunnableGraph}
import akka.stream._
import cats.data.Chain
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{ConfigurableAutomaton, ConfigurableContainer}
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
            inputs: ExternalInputs,
            outputs: ExternalOutputs)
           (implicit locator: PluginLocator): RunnableGraph[_] =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      buildGraph(g, ExternalConnections(inputs, outputs))

      ClosedShape
    })

  def buildGraph(g: Configuration.Graph, external: ExternalConnections)
                (implicit builder: Builder[_],
                 locator: PluginLocator): CompiledGraph = {

    val total = TotalConnections(g.nodes, external)
    val connections = buildInner(g.nodes, g.inners)
  }

  def buildInner(nodes: Seq[Configuration.Node],
                 inners: Map[String, Configuration.Graph])
                (implicit builder: Builder[_],
                 locator: PluginLocator): CompiledGraph = {
    type SeedType = BuildResult[Chain[AutomatonConnections]]
    nodes.map(n => buildNode(n, inners.get(n.id)))
      .foldLeft[SeedType](Right(Chain.empty))(
      foldResults[Chain[AutomatonConnections], AutomatonConnections](_ append _)
    ).map { connections =>
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
        case b: ConfigurableAutomaton => BuildResult.Result(b.build(node))
        case b: ConfigurableContainer => innerGraph.map(g => {
          buildInner(g.nodes, g.inners).flatMap {
            innerConnections =>
              b.build(node) flatMap checkConnections(
                node,
                c => connectAll(
                  innerConnections.inputs,
                  c.intInputs
                ),
                c => connectAll(
                  innerConnections.outputs,
                  c.intOutputs
                )
              )

          }
        }).getOrElse(BuildResult.Error(s"No inner graph for container ${node.id}"))
      }.getOrElse(BuildResult.Error(s"Failed to created instance for $node"))

  def checkConnections[S <: ShapeConnections](node: Configuration.Node,
                                              inConnected: S => Boolean,
                                              outConnected: S => Boolean)(conn: S): BuildResult[AutomatonConnections] =
    (inConnected(conn), outConnected(conn)) match {
      case (true, true) => BuildResult.Result(
        AutomatonConnections(node, conn.inputs, conn.outputs)
      )
      case (false, false) => BuildResult.Error(s"Not all inputs and outputs of graph ${node.id} are connected")
      case (false, _) => BuildResult.Error(s"Not all inputs of graph ${node.id} are connected")
      case (_, false) => BuildResult.Error(s"Not all outputs of graph ${node.id} are connected")
    }
}
