package com.ilyak.pifarm

import akka.stream.scaladsl.GraphDSL
import cats.kernel.Semigroup
import com.ilyak.pifarm.State.GraphState
import com.ilyak.pifarm.flow.configuration.Connection.Sockets

import scala.language.higherKinds

object Types {
  type SMap[T] = Map[String, T]
  type FoldResult[T] = BuildResult[SMap[T]]
  type BuildResult[T] = Either[String, T]

  type HKMapGroup[T[_]] = Semigroup[SMap[T[_]]]
  type MapGroup[T] = Semigroup[SMap[T]]

  type GraphBuilder = GraphDSL.Builder[_]
  type GBuilder[T] = GraphBuilder => T

  type GRun[T] = GraphState => GraphBuilder => (GraphState, T)

  type AddShape = GBuilder[Sockets]

}
