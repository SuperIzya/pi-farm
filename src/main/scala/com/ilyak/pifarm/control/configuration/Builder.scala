package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge, RunnableGraph}
import akka.stream._
import cats.data.Chain
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.{Configuration, Connection}
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
                 inners: TMap[Configuration.Graph])
                (implicit builder: Builder[_],
                 locator: PluginLocator): CompiledGraph = {
    type SeedType = BuildResult[Chain[AutomatonConnections]]
    nodes.map(n => buildNode(n, inners.get(n.id)))
      .foldLeft[SeedType](Right(Chain.empty))(
      foldResults[Chain[AutomatonConnections], AutomatonConnections](_ append _)
    ).map { connections =>
      import cats.implicits._
      import GraphDSL.Implicits._

      val count = connections.map(c =>
        ConnectionsCounter(
          c.inputs.keys.map(_ -> c).toMap,
          c.outputs.keys.map(_ -> c).toMap
        )
      ).foldLeft[ConnectionsMap](ConnectionsCounter.empty)((x, y) => x |+| y)


      val inputs = foldConnections[Connection.In[_], Inlet[_], Broadcast[_]](
        "input",
        count.inputs,
        n => Broadcast[_](n, eagerCancel = false),
        (b, s) => b.shape ~> s.shape
      )

      val outputs = foldConnections[Connection.Out[_], Outlet[_], Merge[_]](
        "output",
        count.outputs,
        n => Merge[_](n, eagerComplete = false),
        (m, s) => s.shape ~> m.shape
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
