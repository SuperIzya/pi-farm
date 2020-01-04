package com.ilyak.pifarm.configuration

import cats.Monoid
import cats.kernel.Semigroup
import com.ilyak.pifarm.flow.configuration.ShapeConnections.AutomatonConnections
import com.ilyak.pifarm.types.SMap

import scala.collection.breakOut

private[configuration] case class ConnectionsCounter[T](inputs: SMap[T],
                                                        outputs: SMap[T])

private[configuration] object ConnectionsCounter {
  import cats.implicits._

  def apply(inputs: Seq[String],
            outputs: Seq[String]): ConnectionsCounter[Int] = {
    val stm: Seq[String] => SMap[Int] = l => l.map(_ -> 1).toMap
    new ConnectionsCounter(stm(inputs), stm(outputs))
  }

  def init[T](init: T,
              inputs: Iterable[String],
              outputs: Iterable[String]): ConnectionsCounter[T] = {
    val ins: SMap[T] = inputs.map(_ -> init)(breakOut)
    val outs: SMap[T] = outputs.map(_ -> init)(breakOut)
    new ConnectionsCounter[T](ins, outs)
  }

  def empty[T]: ConnectionsCounter[T] =
    new ConnectionsCounter(Map.empty, Map.empty)

  implicit val cnCntrSg: Semigroup[ConnectionsCounter[Int]] = (x, y) =>
    ConnectionsCounter[Int](x.inputs |+| y.inputs, x.outputs |+| y.outputs)

  implicit val cnCollSg
    : Semigroup[ConnectionsCounter[List[AutomatonConnections]]] = (x, y) =>
    ConnectionsCounter[List[AutomatonConnections]](
      x.inputs |+| y.inputs,
      x.outputs |+| y.outputs
  )

  implicit def monoidK[T: Monoid]: Monoid[ConnectionsCounter[T]] =
    new Monoid[ConnectionsCounter[T]] {
      override def empty: ConnectionsCounter[T] =
        ConnectionsCounter(Map.empty, Map.empty)

      override def combine(x: ConnectionsCounter[T],
                           y: ConnectionsCounter[T]): ConnectionsCounter[T] =
        ConnectionsCounter(x.inputs |+| y.inputs, x.outputs |+| y.outputs)
    }
}
