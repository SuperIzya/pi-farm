package com.ilyak.pifarm.flow.configuration

import akka.stream._
import cats.~>
import com.ilyak.pifarm.Units

import scala.language.higherKinds

sealed abstract class Connection[T: Units, L[_]] {
  type Let[_] >: L[_]

  val unit: String = Units[T].name
  val name: String

  val let: L[T]
}

object Connection {
  type TConnection[T] = Connection[T, _]

  def apply[T: Units](name: String, shape: SinkShape[T]) = new In[T](name, shape.in)

  def apply[T: Units](name: String, shape: SourceShape[T]) = new Out[T](name, shape.out)

  case class Out[T: Units](name: String, let: Outlet[T]) extends Connection[T, Outlet]

  case class In[T: Units](name: String, let: Inlet[T]) extends Connection[T, Inlet]


  implicit val letOut: Out ~> Outlet = Lambda[Out ~> Outlet](_.let)
  implicit val letIn: In ~> Inlet = Lambda[In ~> Inlet](_.let)

  object External {
    def apply[T: Units](name: String, shape: SourceShape[T]) = new In[T](name, shape)

    def apply[T: Units](name: String, shape: SinkShape[T]) = new Out[T](name, shape)

    case class In[T: Units](name: String, shape: SourceShape[T]) extends Connection[T, SourceShape]

    case class Out[T: Units](name: String, shape: SinkShape[T]) extends Connection[T, SinkShape]

    implicit val letOut: Out ~> Inlet = Lambda[Out ~> Inlet](_.shape.in)
    implicit val letIn: In ~> Outlet = Lambda[In ~> Outlet](_.shape.out)
  }
}

