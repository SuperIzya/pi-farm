package com.ilyak.pifarm

import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.util.ByteString
import cats.Functor
import cats.kernel.Semigroup
import com.ilyak.pifarm.State.GraphState
import com.ilyak.pifarm.driver.DriverCompanion
import com.ilyak.pifarm.flow.configuration.Connection.Sockets
import zio.ZIO

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
  type WrapFlow = Flow[String, String, _] => Flow[String, String, _]

  implicit val functor: Functor[GRun] = new Functor[GRun] {
    override def map[A, B](fa: GRun[A])(f: A => B): GRun[B] = state => b => {
      val (st, a) = fa(state)(b)
      (st, f(a))
    }
  }

  implicit class zioDie[T](val res: Result[T]) extends AnyVal {
    def dieOnError[R, E, U](error: String => String)(fb: T => ZIO[R, E, U]): ZIO[R, E, U] =
      res.fold(s => ZIO.die(new Exception(error(s))), fb)
  }
}
