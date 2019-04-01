package com.ilyak.pifarm.flow.configuration

import akka.stream.scaladsl.Flow
import akka.stream.{FlowShape, Inlet, Outlet}
import cats.Eval
import com.ilyak.pifarm.Types.BuildResult
import com.ilyak.pifarm.Units
import com.ilyak.pifarm.flow.configuration.Connection.{ConnectShape, GBuilder}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{AutomatonConnections, ContainerConnections}

import scala.language.{higherKinds, implicitConversions}

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableNode[S <: ShapeConnections] {
  type TShape = S

  def build(node: Configuration.Node): BuildResult[TShape]
}

object ConfigurableNode {

  /** *
    * Base trait for all plugable [[ConfigurableAutomaton]] type blocks
    */
  trait ConfigurableAutomaton extends ConfigurableNode[AutomatonConnections]

  /** *
    * Base trait for all plugable [[ConfigurableContainer]] type blocks
    *
    */
  trait ConfigurableContainer extends ConfigurableNode[ContainerConnections]


  abstract class FlowAutomaton[I: Units, O: Units] extends ConfigurableAutomaton {

    def flow(conf: Configuration.Node): BuildResult[Flow[I, O, _]]

    final override def build(conf: Configuration.Node): BuildResult[AutomatonConnections] = {
      flow(conf).map(f => {
        val ff: GBuilder[FlowShape[I, O]] = b => Eval.later(b add f)
        val i: GBuilder[Inlet[I]] = b => ff(b).map(_.in)
        val o: GBuilder[Outlet[O]] = b => ff(b).map(_.out)
        AutomatonConnections(
          Seq(Connection.In(conf.inputs.head, i)),
          Seq(Connection.Out(conf.outputs.head, o)),
          ConnectShape.empty,
          conf
        )
      })
    }
  }


  case class Data[T: Units](name: String, unit: Units[T])
  object Data {
    def apply[T: Units](name: String): Data[T] = new Data(name, Units[T])


  }
}
