package com.ilyak.pifarm.control.configuration

import akka.stream._
import akka.stream.scaladsl.{ Broadcast, GraphDSL, Merge, RunnableGraph }
import cats.data.Chain
import cats.kernel.Monoid
import com.ilyak.pifarm.State.GraphState
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{ ConfigurableAutomaton, ConfigurableContainer }
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.flow.configuration.Connection.{ ConnectShape, In, Out }
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{ AutomatonConnections, ContainerConnections, ExternalConnections, ExternalInputs, ExternalOutputs }
import com.ilyak.pifarm.plugins.PluginLocator
import com.ilyak.pifarm.{ BuildResult, State }

import scala.language.{ higherKinds, postfixOps }

/**
  * Transforms [[Configuration.Graph]] to [[RunnableGraph]]
  */
object Builder {

  import BuilderHelpers._
  import State.Implicits._
  import cats.implicits._

  def build(g: Configuration.Graph,
            connections: ExternalConnections)
           (implicit locator: PluginLocator): BuildResult[RunnableGraph[_]] =
    buildGraph(g, connections).map { f =>
      RunnableGraph.fromGraph(GraphDSL.create() { b =>
        f(GraphState.empty)(b)
        ClosedShape
      })
    }

  def build(g: Configuration.Graph,
            inputs: ExternalInputs,
            outputs: ExternalOutputs)
           (implicit locator: PluginLocator): BuildResult[RunnableGraph[_]] =
    build(g, ExternalConnections(inputs, outputs))


  private def buildGraph(g: Configuration.Graph, external: ExternalConnections)
                        (implicit locator: PluginLocator): BuildResult[ConnectShape] = {
    buildInner(g.nodes, g.inners)
    .flatMap { ac =>
      BuildResult.combine(
        ac.inputs.connectExternals(external.inputs),
        ac.outputs.connectExternals(external.outputs)
      )(_ |+| _)
    }
  }

  private def buildInner(nodes: Seq[Configuration.Node],
                         inners: Map[String, Configuration.Graph])
                        (implicit locator: PluginLocator): BuildResult[AutomatonConnections] = {
    type SeedType = BuildResult[Chain[AutomatonConnections]]
    val builtNodes = BuildResult.fold[AutomatonConnections, Chain[AutomatonConnections]](
      nodes.map(n => buildNode(n, inners.get(n.id)))
    )(Chain.empty, _ append _)

    builtNodes.flatMap { connections =>
      import GraphDSL.Implicits._

      val count = connections.map(c =>
        ConnectionsCounter(
          c.inputs.keys.map(_ -> List(c)).toMap,
          c.outputs.keys.map(_ -> List(c)).toMap
        )
      ).foldLeft[ConnectionsMap](ConnectionsCounter.empty)((x, y) => x |+| y)

      val foldedInputs = foldConnections[Inlet, In, UniformFanOutShape[Any, Any]](
        "input",
        count.inputs,
        lst => Broadcast[Any](lst.size),
        (s, l) => implicit b => s ~> l.as[Any]
      )

      val foldedOutputs = foldConnections[Outlet, Out, UniformFanInShape[Any, Any]](
        "output",
        count.outputs,
        lst => Merge[Any](lst.size),
        (s, l) => implicit b => l ~> s
      )

      BuildResult.combineB(foldedInputs, foldedOutputs) {
        connectAll2(_, _)
      }.map { all =>
        val shapes = Monoid[ConnectShape].combineAll(connections.map(_.shape).toList)
        AutomatonConnections(all._1, all._2, shapes |+| all._3)
      }
    }
  }

  private def buildNode(node: Configuration.Node, innerGraph: Option[Configuration.Graph])
                       (implicit locator: PluginLocator): BuildResult[AutomatonConnections] =
    locator.createInstance(node.meta)
    .map {
      case b: ConfigurableAutomaton => b.build(node)
      case b: ConfigurableContainer => innerGraph.map(g => {
        buildInner(g.nodes, g.inners).flatMap {
          inner =>
            b.build(node) flatMap {
              external =>
                connectInner(
                  node,
                  c => inner.inputs.connect(c.intInputs),
                  c => inner.outputs.connect(c.intOutputs),
                  external
                )
            }
        }
      }).getOrElse(BuildResult.Error(s"No inner graph for container ${ node.id }"))
    }.getOrElse(BuildResult.Error(s"Failed to created instance for ${ node.id }"))

  private def connectInner(node: Configuration.Node,
                           connectIn: ContainerConnections => FoldResult[Closed[In]],
                           connectOut: ContainerConnections => FoldResult[Closed[Out]],
                           conn: ContainerConnections): BuildResult[AutomatonConnections] =
    BuildResult.combineB(connectIn(conn), connectOut(conn)) { (ins, outs) =>

      def split[T[_]](m: SMap[Closed[T]]): (List[ConnectShape], SMap[T[_]]) =
        m.foldLeft[(List[ConnectShape], SMap[T[_]])]((List.empty, Map.empty)) {
          (a, e) =>
            e match {
              case (_, l@Left(_)) => (a._1 :+ l.value, a._2)
              case (k, r@Right(_)) => (a._1, a._2 + (k -> r.value))
            }
        }

      def combine(in: List[ConnectShape], out: List[ConnectShape]): ConnectShape =
        Monoid[ConnectShape].combineAll(in ++ out)


      def print[T[_]](m: SMap[T[_]], dir: String): String = {
        if (m.isEmpty) ""
        else s"Non matched $dir connections in ${ node.id }: ${ m.keys }"
      }

      val (inClosed, inOpened) = split(ins)
      val (outClosed, outOpened) = split(outs)

      BuildResult.cond(
        inOpened.isEmpty && outOpened.isEmpty,
        AutomatonConnections(
          conn.inputs,
          conn.outputs,
          combine(inClosed, outClosed),
          node
        ),
        s"""
           |${ print(inOpened, "input") }
           |${ print(outOpened, "output") }
           """.stripMargin
      )
    }
}
