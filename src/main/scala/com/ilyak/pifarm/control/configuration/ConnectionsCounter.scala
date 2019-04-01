package com.ilyak.pifarm.control.configuration

import cats.kernel.Semigroup
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.flow.configuration.ShapeConnections.AutomatonConnections

private[configuration] case class ConnectionsCounter[T](inputs: SMap[T], outputs: SMap[T])

private[configuration] object ConnectionsCounter {
  import cats.implicits._

  def apply(inputs: Seq[String], outputs: Seq[String]): ConnectionsCounter[Int] = {
    val stm: Seq[String] => SMap[Int] = l => l.map(_ -> 1).toMap
    new ConnectionsCounter(stm(inputs), stm(outputs))
  }

  def empty[T]: ConnectionsCounter[T] = new ConnectionsCounter(Map.empty, Map.empty)

  implicit val cnCntrSg: Semigroup[ConnectionsCounter[Int]] = (x, y) =>
    ConnectionsCounter[Int](x.inputs |+| y.inputs, x.outputs |+| y.outputs)

  implicit val cnCollSg: Semigroup[ConnectionsCounter[List[AutomatonConnections]]] = (x, y) =>
    ConnectionsCounter[List[AutomatonConnections]](x.inputs |+| y.inputs, x.outputs |+| y.outputs)
}
