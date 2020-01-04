package com.ilyak.pifarm

import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.util.ByteString
import cats.Monoid
import cats.data.StateT
import cats.implicits._
import cats.kernel.Semigroup
import com.ilyak.pifarm.driver.DriverCompanion
import com.ilyak.pifarm.flow.configuration.Connection.Sockets

import scala.language.higherKinds

package object types {
  type SMap[T] = Map[String, T]
  type Result[T] = Either[String, T]
  type FoldResult[T] = Result[SMap[T]]

  type HKMapGroup[T[_]] = Semigroup[SMap[T[_]]]
  type MapGroup[T] = Semigroup[SMap[T]]

  type GraphBuilder = GraphDSL.Builder[_]
  type GBuilder[T] = GraphBuilder => T

  type GState[T] = StateT[GBuilder, GraphState, T]

  type ConnectState = GState[Unit]

  type GRun[T] = GraphState => GraphBuilder => (GraphState, T)

  type AddShape = GBuilder[Sockets]

  type BinaryConnector = FlowShape[ByteString, ByteString]
  type TDriverCompanion = DriverCompanion.TDriverCompanion
  type WrapFlow = Flow[String, String, _] => Flow[String, String, _]

  implicit val monoidConnectState: Monoid[ConnectState] =
    new Monoid[ConnectState] {
      override def empty: ConnectState = ConnectState.empty

      override def combine(x: ConnectState, y: ConnectState): ConnectState =
        x.flatMap(_ => y)
    }

  implicit def monoidK[T: Monoid]: Monoid[GState[T]] = new Monoid[GState[T]] {
    override def empty: GState[T] = GState.pure(GBuilder.pure(Monoid[T].empty))

    override def combine(x: GState[T], y: GState[T]): GState[T] =
      for {
        a <- x
        b <- y
      } yield a |+| b
  }
}
