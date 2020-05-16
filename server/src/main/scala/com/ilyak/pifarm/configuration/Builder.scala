package com.ilyak.pifarm.configuration

import akka.stream._
import akka.stream.scaladsl.{ Broadcast, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source }
import cats.data.Chain
import cats.kernel.Monoid
import com.ilyak.pifarm.State.GraphState
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{ ConfigurableAutomaton, ConfigurableContainer }
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.flow.configuration.Connection.{ ConnectShape, ExtConnectShape, In, Out }
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{ AutomatonConnections, ContainerConnections, ExternalConnections, ExternalInputs, ExternalOutputs }
import com.ilyak.pifarm.plugins.PluginLocator
import com.ilyak.pifarm.{ Result, State }

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
           (implicit locator: PluginLocator): Result[RunnableGraph[KillSwitch]] = {
    buildGraph(g, connections).map { case (f, c) =>

      val killSwitch = Source.cycle(() => Iterator.single(0))
        .viaMat(KillSwitches.single)(Keep.right)

      RunnableGraph.fromGraph(GraphDSL.create(killSwitch) {
        implicit b => ks =>
          import GraphDSL.Implicits._
          if (c > 0) {
            val kill = b add Broadcast[Int](c)
            ks ~> kill
            f(GraphState(kill))(b)
          } else ks ~> Sink.ignore
          ClosedShape
      })
    }
  }

  def build(g: Configuration.Graph,
            inputs: ExternalInputs,
            outputs: ExternalOutputs)
           (implicit locator: PluginLocator): Result[RunnableGraph[KillSwitch]] =
    build(g, ExternalConnections(inputs, outputs))

  def test(g: Configuration.Graph)
          (implicit locator: PluginLocator): Result[Unit] =
    buildInner(g.nodes, g.inners).map(_ => Unit)

  private def buildGraph(g: Configuration.Graph, external: ExternalConnections)
                        (implicit locator: PluginLocator): Result[ExtConnectShape] =
    buildInner(g.nodes, g.inners)
      .flatMap { ac =>
        Result.combine(
          ac.inputs.connectExternals(external.outputs),
          ac.outputs.connectExternals(external.inputs)
        ){
          case ((a, i), (b, j)) => (ac.shape |+| a |+| b) -> (i + j)
        }
      }

  private def buildInner(nodes: Seq[Configuration.Node],
                         inners: Map[String, Configuration.Graph])
                        (implicit locator: PluginLocator): Result[AutomatonConnections] = {
    type SeedType = Result[Chain[AutomatonConnections]]
    val builtNodes = Result.fold[AutomatonConnections, Chain[AutomatonConnections]](
      nodes.map(n => buildNode(n, inners.get(n.id)))
    )(Chain.empty, _ append _)

    builtNodes.flatMap { connections =>
      import GraphDSL.Implicits._

      val count = connections.map(c =>
        ConnectionsCounter(
          c.inputs.keys.map(_ -> List(c)).toMap,
          c.outputs.keys.map(_ -> List(c)).toMap
        )
      ).foldLeft[ConnectionsMap](ConnectionsCounter.empty)(_ |+| _)

      val foldedInputs = foldConnections[Inlet, In, UniformFanOutShape[Any, Any]](
        "input",
        count.inputs,
        n => Broadcast[Any](n),
        (s, l) => implicit b => s ~> l.as[Any]
      )

      val foldedOutputs = foldConnections[Outlet, Out, UniformFanInShape[Any, Any]](
        "output",
        count.outputs,
        n => Merge[Any](n),
        (s, l) => implicit b => l ~> s
      )

      Result.combineB(foldedInputs, foldedOutputs) {
        interConnect
      }.map { case (ins, outs, shape) =>
        val shapes = Monoid[ConnectShape].combineAll(connections.map(_.shape).toList)
        AutomatonConnections(ins, outs, shapes |+| shape)
      }
    }
  }

  private def buildNode(node: Configuration.Node, innerGraph: Option[Configuration.Graph])
                       (implicit locator: PluginLocator): Result[AutomatonConnections] =
    locator.createInstance(node.meta)
      .map {
        case b: ConfigurableAutomaton => b.build(node)
        case b: ConfigurableContainer => innerGraph.map(g => {
          buildInner(g.nodes, g.inners).flatMap {
            inner =>
              b.build(node, inner) flatMap {
                external =>
                  connectInner(
                    node,
                    inner.inputs.connect(external.intInputs),
                    inner.outputs.connect(external.intOutputs),
                    external,
                    external.shape |+| inner.shape
                  )
              }
          }
        }).getOrElse(Result.Err(s"No inner graph for container ${ node.id }"))
      }.getOrElse(Result.Err(s"Failed to created instance for ${ node.id }"))

  private def connectInner(node: Configuration.Node,
                           connectIn: FoldResult[Closed[In]],
                           connectOut: FoldResult[Closed[Out]],
                           conn: ContainerConnections,
                           shape: ConnectShape): Result[AutomatonConnections] =
    Result.combineB(connectIn, connectOut) { (ins, outs) =>

      def split[T[_]](m: SMap[Closed[T]]): (List[ConnectShape], SMap[T[_]]) =
        m.foldLeft[(List[ConnectShape], SMap[T[_]])]((List.empty, Map.empty)) {
          (a, e) =>
            e match {
              case (_, l@Left(_)) => (a._1 :+ l.value, a._2)
              case (k, r@Right(_)) => (a._1, a._2 + (k -> r.value))
            }
        }

      def print[T[_]](m: SMap[T[_]], dir: String): String = {
        if (m.isEmpty) ""
        else s"Non matched $dir connections in ${ node.id }: ${ m.keys }"
      }

      val (inClosed, inOpened) = split(ins)
      val (outClosed, outOpened) = split(outs)

      Result.cond(
        inOpened.isEmpty && outOpened.isEmpty,
        AutomatonConnections(
          conn.inputs,
          conn.outputs,
          shape |+| Monoid[ConnectShape].combineAll(inClosed ++ outClosed),
          node
        ),
        s"""
           |${ print(inOpened, s"input") }
           |${ print(outOpened, "output") }
           """.stripMargin
      )
    }
}
