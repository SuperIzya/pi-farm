package com.ilyak.pifarm.control.configuration

import cats.kernel.Semigroup
import com.ilyak.pifarm.control.configuration.BuilderHelpers.ConnCounter
import com.ilyak.pifarm.flow.configuration.ShapeConnections.AutomatonConnections

private[configuration] case class ConnectionsCounter[T](inputs: ConnCounter[T], outputs: ConnCounter[T])

private[configuration] object ConnectionsCounter {
  import cats.implicits._

  def apply(inputs: Seq[String], outputs: Seq[String]): ConnectionsCounter[Int] = {
    val stm: Seq[String] => ConnCounter[Int] = l => l.map(_ -> 1).toMap
    new ConnectionsCounter(stm(inputs), stm(outputs))
  }

  def apply(inputs: Map[String, AutomatonConnections],
            outputs: Map[String, AutomatonConnections]): ConnectionsCounter[List[AutomatonConnections]] =
    new ConnectionsCounter(
      inputs.map(x => x._1 -> List(x._2)),
      outputs.map(x => x._1 -> List(x._2))
    )

  def empty[T]: ConnectionsCounter[T] = new ConnectionsCounter(Map.empty, Map.empty)

  implicit val cnCntrSg: Semigroup[ConnectionsCounter[Int]] = (x, y) =>
    ConnectionsCounter[Int](x.inputs |+| y.inputs, x.outputs |+| y.outputs)

  implicit val cnCollSg: Semigroup[ConnectionsCounter[List[AutomatonConnections]]] = (x, y) =>
    ConnectionsCounter[List[AutomatonConnections]](x.inputs |+| y.inputs, x.outputs |+| y.outputs)
}
