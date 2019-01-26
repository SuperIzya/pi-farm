package com.ilyak.pifarm.flow.configuration

import akka.stream.{FlowShape, Shape}
import akka.stream.scaladsl.{Flow, GraphDSL}
import com.ilyak.pifarm.flow.Messages.Data
import com.ilyak.pifarm.flow.configuration.ConfigurableShape._

/** *
  * Transforms [[ConfigurableShape]] to [[Shape]]
  *
  * @tparam C type of configuration shape
  * @tparam S type of the result [[Shape]]
  */
trait ShapeTransformer[-C[_] <: ConfigurableShape[_]] {
  /** *
    * Returns [[ShapeConnections]] with names of all connections to the outside world.
    *
    * @param conf     : [[com.ilyak.pifarm.flow.configuration.Configuration.Node]] from configuration
    * @param shapeObj : [[ConfigurableShape#ShapeObject]] produced from conf
    * @tparam D : Concrete type of [[ConfigurableShape]]
    * @return
    */
  def getOutConnections[D[_] <: C[_]](conf: Configuration.Node, shapeObj: D#ShapeObject): ShapeConnections
}

object ShapeTransformer {

  trait AutomatonTransformer[-A[_] <: ConfigurableAutomaton[_], S <: Shape] extends ShapeTransformer[A] {
    def transform[C[_] <: A[_]](conf: C[S])(implicit builder: GraphDSL.Builder[_]): C#ShapeObject[C[_]]
  }

  trait ContainerTransformer[-A[_] <: ConfigurableContainer[_], S <: Shape] extends ShapeTransformer[A] {
    def transform[C[_] <: A[_]](conf: C[S])(implicit builder: GraphDSL.Builder[_]): C#ShapeObject[C[_]]

    /** *
      * Returns [[ExternalConnections]] for enclosed [[Shape]]
      *
      * @param conf     : [[com.ilyak.pifarm.flow.configuration.Configuration.Node]] from configuration
      * @param shapeObj : [[ConfigurableShape#ShapeObject]] produced from @conf
      * @tparam C : Concrete type of [[ConfigurableContainer]]
      * @return
      */
    def getInConnections[C[_] <: A[_]](conf: Configuration.Node, shapeObj: C#ShapeObject): ExternalConnections
  }

  trait FlowAutomatonTransformer[-A[_] <: ConfigurableAutomaton[_], S <: FlowShape[I, O], I <: Data, O <: Data]
   extends AutomatonTransformer[A, S] {
    def flow: Flow[I, O, _]

    override def transform[C[_] <: A[_]](conf: C[S])(implicit builder: GraphDSL.Builder[_]): C#ShapeObject[C[_], S] =
      // TODO: Fix it
      conf.wrap(builder.add(flow))
  }
}