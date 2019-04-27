package com.ilyak.pifarm

import akka.stream.{ FlowShape, KillSwitch }
import akka.stream.scaladsl.{ Flow, GraphDSL }
import akka.util.ByteString
import cats.Functor
import cats.kernel.Semigroup
import com.ilyak.pifarm.State.GraphState
import com.ilyak.pifarm.driver.DriverCompanion
import com.ilyak.pifarm.flow.configuration.Connection.Sockets

import scala.language.higherKinds

object Types {
  type SMap[T] = Map[String, T]
  type Result[T] = Either[String, T]
  type FoldResult[T] = Result[SMap[T]]

  type HKMapGroup[T[_]] = Semigroup[SMap[T[_]]]
  type MapGroup[T] = Semigroup[SMap[T]]

  type GraphBuilder = GraphDSL.Builder[_]
  type GBuilder[T] = GraphBuilder => T

  type GRun[T] = GraphState => GraphBuilder => (GraphState, T)

  type AddShape = GBuilder[Sockets]

  type BinaryConnector = FlowShape[ByteString, ByteString]
  type TDriverCompanion = DriverCompanion.TDriverCompanion
  type WrapFlow = Flow[String, String, KillSwitch] => Flow[String, String, KillSwitch]

  implicit val functor: Functor[GRun] = new Functor[GRun] {
    override def map[A, B](fa: GRun[A])(f: A => B): GRun[B] = state => b => {
      val (st, a) = fa(state)(b)
      (st, f(a))
    }
  }
}
