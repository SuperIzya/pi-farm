package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream._
import akka.stream.stage.GraphStage
import cats.data.Chain
import com.ilyak.pifarm.control.configuration.TotalConnections.SeedType
import com.ilyak.pifarm.flow.Messages.{AggregateCommand, Data, SensorData}
import com.ilyak.pifarm.flow.configuration.ShapeBuilder.{AutomatonBuilder, ContainerBuilder}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ExternalConnections, InputSocket, OutputSocket}
import com.ilyak.pifarm.flow.configuration._
import com.ilyak.pifarm.plugins.PluginLocator

import scala.language.higherKinds

/**
  * Transforms [[Configuration.Graph]] to [[RunnableGraph]]
  */
object Builder {

  import BuilderTypes._


  def build(g: Configuration.Graph,
            inputs: Map[String, Source[SensorData[_], _]],
            outputs: Map[String, Sink[AggregateCommand, _]])
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

    nodes.map(n => buildNode(n, inners.get(n.id)))
      .foldLeft[SeedType](Right(Chain.empty))(
      foldResults[Chain[AutomatonConnections], AutomatonConnections](_ append _)
    ).map { connections =>
      import cats.implicits._
      import GraphDSL.Implicits._

      val count: ConnectionsMap = connections.map(c =>
        ConnectionsCounter(
          c.inputs.keys.map(_ -> c).toMap,
          c.outputs.keys.map(_ -> c).toMap
        )
      ).foldLeft[ConnectionsMap](ConnectionsCounter.empty)((x, y) => x |+| y)


      val inputs = foldConnections[InputSocket[_ <: Data], Inlet[_ <: Data], Broadcast[Data]](
        "input",
        _.inputs,
        count.inputs,
        _.in,
        c => Broadcast[Data](c.size, eagerCancel = false),
        (b, s) => {
          b.shape ~> s.in
          true
        },
        _.in
      )

      val outputs = foldConnections[OutputSocket[_ <: Data], Outlet[_ <: Data], Merge[Data]](
        "output",
        _.outputs,
        count.outputs,
        _.out,
        c => Merge[Data](c.size, eagerComplete = false),
        (m, s) => {
          s.out ~> m.shape
          true
        },
        _.out
      )
    }
  }


  def buildNode(node: Configuration.Node, innerGraph: Option[Configuration.Graph])
               (implicit locator: PluginLocator,
                builder: Builder[_]): BuildResult[AutomatonConnections] = {

    import GraphDSL.Implicits._

    locator.createInstance(node.meta)
      .map {
        case b: AutomatonBuilder => Right(b.build(node))
        case b: ContainerBuilder => innerGraph.map(g => {
          buildInner(g.nodes, g.inners).flatMap {
            innerConnections =>
              val conn = b.build(node)

              val inputs = areAllConnected(
                innerConnections.inputs,
                conn.intInputs,
                (is: InputSocket[_], s: OutputSocket[_]) => {
                  s.out ~> is.in
                  true
                }
              )

              val outputs = areAllConnected(
                innerConnections.outputs,
                conn.intOutputs,
                (os: OutputSocket[_], s: InputSocket[_]) => {
                  os.out ~> s.in
                  true
                }
              )

              (inputs, outputs) match {
                case (true, true) => Right(AutomatonConnections(node, conn.inputs, conn.outputs))
                case (false, false) => Left(s"Not all inputs and outputs of graph ${node.id} are connected")
                case (false, _) => Left(s"Not all inputs of graph ${node.id} are connected")
                case (_, false) => Left(s"Not all outputs of graph ${node.id} are connected")
              }
          }
        }).getOrElse(Left(s"No inner graph for container ${node.id}"))
      }.getOrElse(Left(s"Failed to created instance for $node"))
  }

}
