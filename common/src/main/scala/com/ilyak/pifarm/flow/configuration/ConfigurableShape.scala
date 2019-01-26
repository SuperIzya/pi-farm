package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import cats.Monoid
import shapeless.T

import scala.language.implicitConversions

/** *
  * Base interface for all plugable blocks.
  */
trait ConfigurableShape[S <: Shape] {

  case class ShapeObject[C[_] <: ConfigurableShape[_], T <: S](shape: T, conf: C[T])

  object ShapeObject {
    implicit def toS(obj: ShapeObject): S = obj.shape
  }

  /** * returns new [[Shape]] according to this configuration */
  def build(builder: BlockBuilder): ShapeObject[this.type, S]

  /** *
    * returns [[Sink]] which is connected to internal [[Source]]
    *
    * @param shape      : [[ShapeObject]] containing shape produced by this [[ConfigurableShape]]
    * @param connection : id of the connection
    */
  def getInput(shape: ShapeObject[_, _], connection: String): Sink[_, _]

  /** *
    * returns [[Source]] which is connected to internal [[Sink]]
    *
    * @param shape      : [[ShapeObject]] containing shape produced by this [[ConfigurableShape]]
    * @param connection : id of the connection
    */
  def getOutput(shape: ShapeObject[_, _], connection: String): Source[_, _]

  def wrap[C[_] <: ConfigurableShape[_], T <: S](shape: T): this.ShapeObject[C, T] =
    ShapeObject(shape, this.asInstanceOf[C[T]])
}

object ConfigurableShape {

  /** *
    * Base trait for all plugable [[ConfigurableAutomaton]] type blocks
    */
  trait ConfigurableAutomaton[S <: Shape]
    extends ConfigurableShape[S]

  /** *
    * Base trait for all plugable [[ConfigurableContainer]] type blocks
    *
    * @tparam S - type of [[Shape]]
    */
  trait ConfigurableContainer[S <: Shape]
    extends ConfigurableShape[S]

  /** *
    * Connections to/from the outside world needed by the shape.
    *
    * @param inputs  : ids of inbound connections
    * @param outputs : ids of outbound connections
    */
  case class ShapeConnections(inputs: List[String],
                              outputs: List[String])

  object ShapeConnections {
    val empty = ShapeConnections(List.empty, List.empty)

    implicit val connectionsMonoid = new Monoid[ShapeConnections] {

      import cats.implicits._

      override def empty: ShapeConnections = ShapeConnections.empty

      override def combine(x: ShapeConnections, y: ShapeConnections): ShapeConnections = {
        ShapeConnections(x.inputs |+| y.inputs, x.outputs |+| y.outputs)
      }
    }
  }

  /** *
    * Connections to/from the outside world external to the shape.
    *
    * @param inputs  : id -> [[Source]] for inbound connections
    * @param outputs : id -> [[Sink]] for outbound connections
    */
  case class ExternalConnections(inputs: Map[String, Source[_, _]], outputs: Map[String, Sink[_, _]])

  object ExternalConnections {
    val empty = ExternalConnections(Map.empty, Map.empty)
  }

}
