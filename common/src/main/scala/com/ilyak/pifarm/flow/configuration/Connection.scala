package com.ilyak.pifarm.flow.configuration

import akka.stream._
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

  class Out[T: Units](val name: String, val shape: SourceShape[T]) extends Connection[T, SourceShape]

  class In[T: Units](val name: String, val shape: SinkShape[T]) extends Connection[T, SinkShape]

  object External {
    def apply[T: Units](name: String, shape: SourceShape[T]) = new In[T](name, shape)
    def apply[T: Units](name: String, shape: SinkShape[T]) = new Out[T](name, shape)

    class In[T: Units](val name: String, val shape: SourceShape[T]) extends Connection[T, SourceShape]
    class Out[T: Units](val name: String, val shape: SinkShape[T]) extends Connection[T, SinkShape]
  }

  trait XLet[C <: Connection[_, _], L] {
    def apply(c: C): L
  }

  trait GetLet[C <: Connection[_, _]] extends XLet[C, _]

  object XLet {
    def apply[C <: Connection[_, _], L](implicit x: XLet[C, _]): XLet[C, _] = x
  }

  implicit val letOut = new XLet[Out[_], Outlet[_]] {
    override def apply(c: Out[_]): Outlet[_] = c.shape.out
  }
  implicit val letIn = new XLet[In[_], Inlet[_]] {
    override def apply(c: In[_]): Inlet[_] = c.shape.in
  }

}

