package com.ilyak.pifarm.control.configuration

import cats.kernel.Semigroup
import com.ilyak.pifarm.Build.TMap
import com.ilyak.pifarm.flow.configuration.ShapeConnections.AutomatonConnections

private[configuration] case class ConnectionsCounter[T](inputs: TMap[T], outputs: TMap[T])

private[configuration] object ConnectionsCounter {
  import cats.implicits._

  def apply(inputs: Seq[String], outputs: Seq[String]): ConnectionsCounter[Int] = {
    val stm: Seq[String] => TMap[Int] = l => l.map(_ -> 1).toMap
    new ConnectionsCounter(stm(inputs), stm(outputs))
  }

  def empty[T]: ConnectionsCounter[T] = new ConnectionsCounter(Map.empty, Map.empty)

  implicit val cnCntrSg: Semigroup[ConnectionsCounter[Int]] = (x, y) =>
    ConnectionsCounter[Int](x.inputs |+| y.inputs, x.outputs |+| y.outputs)

  implicit val cnCollSg: Semigroup[ConnectionsCounter[List[AutomatonConnections]]] = (x, y) =>
    ConnectionsCounter[List[AutomatonConnections]](x.inputs |+| y.inputs, x.outputs |+| y.outputs)
}
