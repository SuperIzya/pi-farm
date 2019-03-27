package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import cats.{Monoid, ~>}
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.Units
import com.ilyak.pifarm.flow.configuration.Connection.{ConnectShape, TConnection}

import scala.language.higherKinds


sealed trait Connection[T, L[_]] extends TConnection[T] {
  type Let[_] = L[_]
  val let: L[T]
  val connect: ConnectShape
}

object Connection {

  sealed trait TConnection[T] {
    val unit: String
    val name: String
  }

  type ConnectShape = GraphDSL.Builder[_] => Unit

  object ConnectShape {
    val empty: ConnectShape = _ => Unit

    private def tryConnect[C[_] <: TConnection[_], D[_] <: TConnection[_]]
    (x: C[_], y: D[_], connect: => ConnectShape): BuildResult[ConnectShape] = {
      BuildResult.cond(
        x.unit == y.unit,
        connect,
        s"Wrong units (${x.name}:${x.unit} -> ${y.name}:${y.unit})"
      )
    }

    def apply(in: In[_], out: Out[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      tryConnect(out, in, implicit b => out.let ~> in.let.as[Any])
    }

    def apply(out: Out[_], in: In[_]): BuildResult[ConnectShape] = apply(in, out)

    def apply(in: In[_], extIn: External.In[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      tryConnect(in, extIn, implicit b => extIn.let ~> in.let.as[Any])
    }

    def apply(out: Out[_], extOut: External.Out[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      tryConnect(out, extOut, implicit b => out.let ~> extOut.let.as[Any])
    }

    implicit val monad: Monoid[ConnectShape] = new Monoid[ConnectShape] {
      override def empty: ConnectShape = ConnectShape.empty

      override def combine(x: ConnectShape, y: ConnectShape): ConnectShape = (x, y) match {
        case (ConnectShape.empty, ConnectShape.empty) => ConnectShape.empty
        case (ConnectShape.empty, a) => a
        case (a, ConnectShape.empty) => a
        case (c, d) => b => c(b); d(b)
      }
    }
  }

  // TODO: Move all apply methods to trait
  def apply[T: Units](in: Inlet[T]): In[T] = apply(in, ConnectShape.empty)
  def apply[T: Units](in: Inlet[T], connect: ConnectShape): In[T] = apply(in.s, in, connect)

  def apply[T: Units](name: String, in: Inlet[T]): In[T] = apply(name, in, ConnectShape.empty)
  def apply[T: Units](name: String, in: Inlet[T], connect: ConnectShape): In[T] =
    new In[T](name, in, Units[T].name, connect)

  def apply[T: Units](name: String, shape: SinkShape[T]): In[T] = apply(name, shape, ConnectShape.empty)
  def apply[T: Units](name: String, shape: SinkShape[T], connect: ConnectShape): In[T] =
    new In[T](name, shape.in, Units[T].name, connect)

  def apply[T: Units](name: String, flow: Sink[T, _]): In[T] = apply(name, flow, ConnectShape.empty)
  def apply[T: Units](name: String, flow: Sink[T, _], connect: ConnectShape): In[T] =
    new In[T](name, flow.shape.in, Units[T].name, connect)

  def apply[T: Units](out: Outlet[T]): Out[T] = apply(out, ConnectShape.empty)
  def apply[T: Units](out: Outlet[T], connect: ConnectShape): Out[T] = apply(out.s, out, connect)

  def apply[T: Units](name: String, out: Outlet[T]): Out[T] = apply(name, out, ConnectShape.empty)
  def apply[T: Units](name: String, out: Outlet[T], connect: ConnectShape): Out[T] =
    new Out[T](name, out, Units[T].name, connect)

  def apply[T: Units](name: String, flow: Source[T, _]): Out[T] = apply(name, flow, ConnectShape.empty)
  def apply[T: Units](name: String, flow: Source[T, _], connect: ConnectShape): Out[T] =
    new Out[T](name, flow.shape.out, Units[T].name, connect)

  def apply[T: Units](name: String, shape: SourceShape[T]): Out[T] = apply(name, shape, ConnectShape.empty)
  def apply[T: Units](name: String, shape: SourceShape[T], connect: ConnectShape): Out[T] =
    new Out[T](name, shape.out, Units[T].name, connect)

  case class Out[T: Units](name: String, let: Outlet[T], unit: String, connect: ConnectShape)
    extends Connection[T, Outlet]

  case class In[T: Units](name: String, let: Inlet[T], unit: String, connect: ConnectShape)
    extends Connection[T, Inlet]

  implicit val letOut: Out ~> Outlet = Lambda[Out ~> Outlet](_.let)
  implicit val letIn: In ~> Inlet = Lambda[In ~> Inlet](_.let)

  object External {
    def apply[T: Units](out: Outlet[T]) = new In[T](out.s, out, Units[T].name)

    def apply[T: Units](name: String, s: SourceShape[T]) = new In[T](name, s.out, Units[T].name)

    def apply[T: Units](name: String, s: Source[T, _]) = new In[T](name, s.shape.out, Units[T].name)

    def apply[T: Units](in: Inlet[T]) = new Out[T](in.s, in, Units[T].name)

    def apply[T: Units](name: String, s: Sink[T, _]) = new Out[T](name, s.shape.in, Units[T].name)

    def apply[T: Units](name: String, s: SinkShape[T]) = new Out[T](name, s.in, Units[T].name)

    case class In[T: Units](name: String, let: Outlet[T], unit: String, connect: ConnectShape = ConnectShape.empty)
      extends Connection[T, Outlet]

    case class Out[T: Units](name: String, let: Inlet[T], unit: String, connect: ConnectShape = ConnectShape.empty)
      extends Connection[T, Inlet]

    implicit val letOut: Out ~> Inlet = Lambda[Out ~> Inlet](_.let)
    implicit val letIn: In ~> Outlet = Lambda[In ~> Outlet](_.let)
  }


  trait ConnectF[C[_], D[_]] {
    def apply(c: C[_], d: D[_]): BuildResult[ConnectShape]
  }

  implicit val Cio: ConnectF[In, Out] = ConnectShape(_, _)
  implicit val Coi: ConnectF[Out, In] = ConnectShape(_, _)
  implicit val Ceio: ConnectF[In, External.In] = ConnectShape(_, _)
  implicit val Ceoi: ConnectF[Out, External.Out] = ConnectShape(_, _)
}

