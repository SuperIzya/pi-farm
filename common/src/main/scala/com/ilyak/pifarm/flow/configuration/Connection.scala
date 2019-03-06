package com.ilyak.pifarm.flow.configuration

import akka.stream._
import cats.~>

import com.ilyak.pifarm.Units

import scala.language.higherKinds

sealed abstract class Connection[T: Units, S[_] <: Shape] {
  val unit: String = Units[T].name
  val name: String
  val shape: S[T]
}

object Connection {
  def apply[T: Units](name: String, shape: SinkShape[T]) = new In[T](name, shape)

  def apply[T: Units](name: String, shape: SourceShape[T]) = new Out[T](name, shape)

  case class Out[T: Units](name: String, shape: SourceShape[T]) extends Connection[T, SourceShape]

  case class In[T: Units](name: String, shape: SinkShape[T]) extends Connection[T, SinkShape]

  type TCon[T] = Connection[T, _]


  trait XLet[C[_] <: Connection[_, _], L[_]] {
    def apply(c: C): L[_]
  }
  type TLet[C[_] <: Connection[_, _]] = XLet[C, _]

  object TLet {
    def apply[C[_] <: Connection[_, _], L[_]: XLet[C, _]]: XLet[C, L] = implicitly[XLet[C, L]]
  }

  implicit val letOut: TLet[Out] = _.shape.out
  implicit val letIn: TLet[In] = _.shape.in

  object External {
    def apply[T: Units](name: String, shape: SourceShape[T]) = new In[T](name, shape)

    def apply[T: Units](name: String, shape: SinkShape[T]) = new Out[T](name, shape)

    case class In[T: Units](name: String, shape: SourceShape[T]) extends Connection[T, SourceShape]

    case class Out[T: Units](name: String, shape: SinkShape[T]) extends Connection[T, SinkShape]

    implicit val letOut: TLet[Out] = _.shape.in
    implicit val letIn: TLet[In] = _.shape.out
  }
}

