package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import cats.{Monoid, ~>}
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.Units
import com.ilyak.pifarm.flow.configuration.Connection.{Connect, TConnection}

import scala.language.higherKinds


sealed trait Connection[T, L[_]] extends TConnection[T] {
  type Let[_] = L[_]
  val let: L[T]
  val connect: Connect
}

object Connection {

  sealed trait TConnection[T] {
    val unit: String
    val name: String
  }

  type Connect = GraphDSL.Builder[_] => Unit

  object Connect {
    val empty: Connect = _ => Unit

    private def tryConnect[C[_] <: TConnection[_], D[_] <: TConnection[_]]
    (x: C[_], y: D[_], connect: => Connect): BuildResult[Connect] = {
      BuildResult.cond(
        x.unit == y.unit,
        connect,
        s"Wrong units (${x.name}:${x.unit} -> ${y.name}:${y.unit})"
      )
    }

    def apply(in: In[_], out: Out[_]): BuildResult[Connect] = {
      import GraphDSL.Implicits._
      tryConnect(out, in, implicit b => out.let ~> in.let.as[Any])
    }

    def apply(out: Out[_], in: In[_]): BuildResult[Connect] = apply(in, out)

    def apply(in: In[_], extIn: External.In[_]): BuildResult[Connect] = {
      import GraphDSL.Implicits._
      tryConnect(in, extIn, implicit b => extIn.let ~> in.let.as[Any])
    }

    def apply(out: Out[_], extOut: External.Out[_]): BuildResult[Connect] = {
      import GraphDSL.Implicits._
      tryConnect(out, extOut, implicit b => out.let ~> extOut.let.as[Any])
    }

    implicit val monad: Monoid[Connect] = new Monoid[Connect] {
      override def empty: Connect = Connect.empty

      override def combine(x: Connect, y: Connect): Connect = (x, y) match {
        case (Connect.empty, Connect.empty) => Connect.empty
        case (Connect.empty, a) => a
        case (a, Connect.empty) => a
        case (c, d) => b => c(b); d(b)
      }
    }
  }

  // TODO: Move all apply methods to trait
  def apply[T: Units](in: Inlet[T]): In[T] = apply(in, Connect.empty)
  def apply[T: Units](in: Inlet[T], connect: Connect): In[T] = apply(in.s, in, connect)

  def apply[T: Units](name: String, in: Inlet[T], connect: Connect = Connect.empty): In[T] =
    new In[T](name, in, Units[T].name, connect)

  def apply[T: Units](name: String, shape: SinkShape[T], connect: Connect = Connect.empty): In[T] =
    new In[T](name, shape.in, Units[T].name, connect)

  def apply[T: Units](name: String, flow: Sink[T, _], connect: Connect = Connect.empty): In[T] =
    new In[T](name, flow.shape.in, Units[T].name, connect)


  def apply[T: Units](out: Outlet[T], connect: Connect = Connect.empty): Out[T] =
    apply(out.s, out, connect)

  def apply[T: Units](name: String, out: Outlet[T], connect: Connect = Connect.empty): Out[T] =
    new Out[T](name, out, Units[T].name, connect)

  def apply[T: Units](name: String, flow: Source[T, _], connect: Connect = Connect.empty): Out[T] =
    new Out[T](name, flow.shape.out, Units[T].name, connect)

  def apply[T: Units](name: String, shape: SourceShape[T], connect: Connect = Connect.empty): Out[T] =
    new Out[T](name, shape.out, Units[T].name, connect)

  case class Out[T: Units](name: String, let: Outlet[T], unit: String, connect: Connect)
    extends Connection[T, Outlet]

  case class In[T: Units](name: String, let: Inlet[T], unit: String, connect: Connect)
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

    case class In[T: Units](name: String, let: Outlet[T], unit: String, connect: Connect = Connect.empty)
      extends Connection[T, Outlet]

    case class Out[T: Units](name: String, let: Inlet[T], unit: String, connect: Connect = Connect.empty)
      extends Connection[T, Inlet]

    implicit val letOut: Out ~> Inlet = Lambda[Out ~> Inlet](_.let)
    implicit val letIn: In ~> Outlet = Lambda[In ~> Outlet](_.let)
  }


  trait ConnectF[C[_], D[_]] {
    def apply(c: C[_], d: D[_]): BuildResult[Connect]
  }

  implicit val Cio: ConnectF[In, Out] = Connect(_, _)
  implicit val Coi: ConnectF[Out, In] = Connect(_, _)
  implicit val Ceio: ConnectF[In, External.In] = Connect(_, _)
  implicit val Ceoi: ConnectF[Out, External.Out] = Connect(_, _)
}

