package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, Shape}
import com.ilyak.pifarm.flow.Messages.{AggregateCommand, Data, SensorData}
import com.ilyak.pifarm.flow.configuration.BlockBuilder.{BuiltNode, CompiledGraph}
import com.ilyak.pifarm.flow.configuration.ConfigurableShape._
import com.ilyak.pifarm.flow.configuration.ShapeTransformer.{AutomatonTransformer, ContainerTransformer}
import com.ilyak.pifarm.flow.configuration._
import com.ilyak.pifarm.plugins.PluginLocator

import scala.language.higherKinds

/**
  * Transforms [[Configuration.Graph]] to [[RunnableGraph]]
  */
object Builder {

  import BlockBuilder.BuildResult

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

    val res = g.nodes
      .map(n => buildNode(n, g.inners.get(n.id)))
      .foldLeft(total.seed)((b, r) => (b, r) match {
        case (Right(lst), Right(cn)) => Right(lst append cn)
        case (Left(l1), Left(l2)) => Left(
          s"""
             |$l1
             |$l2
            """.stripMargin)
        case (l: Left[_, _], _) => l
        case (_, Left(l)) => Left(l)
      }).map(_.toList)

    val inputs = total.connCounter
      .inputs
      .map {
        case (name: String, 1) => name -> None
        case (name: String, count: Int) =>
          name -> Some(builder.add(new Broadcast[Data](count, false)))
      }

    val outputs = total.connCounter
        .outputs
        .map {
          case (name: String, 1) => name -> None
          case (name: String, count: Int) =>
            name -> Some(builder.add(new Merge[Data](count, false)))
        }

    res.map(_.foreach(r =>
// TODO: Connect all nodes
    ))

    res
  }

  def buildNode(node: Configuration.Node, inner: Option[Configuration.Graph])
               (implicit locator: PluginLocator,
                builder: Builder[_]): BuildResult[BuiltNode] =
    locator.createInstance(node.meta)
      .map(block => Right(BuiltNode(node, block.build(new Helper(node, inner)))))
      .getOrElse(Left(s"Failed to created instance for $node"))

  private class Helper(node: Configuration.Node,
                       g: Option[Configuration.Graph])
                      (implicit builder: GraphDSL.Builder[_],
                       locator: PluginLocator) extends BlockBuilder {

    override def buildAutomaton[A[_] <: ConfigurableAutomaton[_], S <: Shape]
    (automaton: A[S])(implicit st: AutomatonTransformer[A[S], S]): BuildResult[BuiltNode] = {
      val shape = st.transform(automaton)
      Right(BuiltNode(node, shape))
    }

    override def buildContainer[C[_] <: ConfigurableContainer[_], S <: Shape]
    (container: C[S])(implicit st: ContainerTransformer[C[S], S]): BuildResult[BuiltNode] = {
      g.flatMap(_.inners.get(node.id)) match {
        case None => Left(s"Container ${node.id} has no inner graph")
        case Some(inner) =>
          val shape = st.transform(container)
          val innerConn = st.getInConnections(node, shape)
          Builder
            .buildGraph(inner, innerConn)
            .map(_ => BuiltNode(node, shape))
      }
    }
  }

}
