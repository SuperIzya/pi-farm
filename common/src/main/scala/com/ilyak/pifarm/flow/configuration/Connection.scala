package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import cats.~>
import com.ilyak.pifarm.Units

import scala.language.higherKinds

sealed trait TConnection[T] {
  val unit: String
  val name: String
}

sealed trait Connection[T, L[_]] extends TConnection[T] {
  type Let[_] = L[_]
  val let: L[T]
}

object Connection {

  type Flw[T: Units] = Flow[T, T, _]
  object Flw {
    def toGraph[S <: Shape](s: => S): Graph[S, _] = GraphDSL.create(){ _ => s}
    def apply(in: Inlet[_], out: Outlet[_]): Flw[_] =
      Flow.fromGraph(GraphDSL.create(){implicit b =>
        import GraphDSL.Implicits._
        out ~> in
        FlowShape(in, out)
      })


    def apply(in: In[_], out: Out[_]): Flw[_] = Flw(in.let, out.let)
    def apply(out: Out[_], in: In[_]): Flw[_] = Flw(in.let, out.let)
  }

  def apply[T: Units](name: String, shape: SinkShape[T]) = new In[T](name, shape.in, Units[T].name)
  def apply[T: Units](name: String, flow: Sink[T, _]) = new In[T](name, flow.shape.in, Units[T].name)

  def apply[T: Units](name: String, flow: Source[T, _]) = new Out[T](name, flow.shape.out, Units[T].name)
  def apply[T: Units](name: String, shape: SourceShape[T]) = new Out[T](name, shape.out, Units[T].name)

  case class Out[T: Units](name: String, let: Outlet[T], unit: String) extends Connection[T, Outlet]

  case class In[T: Units](name: String, let: Inlet[T], unit: String) extends Connection[T, Inlet]

  implicit val letOut: Out ~> Outlet = Lambda[Out ~> Outlet](_.let)
  implicit val letIn: In ~> Inlet = Lambda[In ~> Inlet](_.let)

  object External {
    def apply[T: Units](name: String, s: SourceShape[T]) = new In[T](name, s.out, Units[T].name)
    def apply[T: Units](name: String, s: Source[T, _]) = new In[T](name, s.shape.out, Units[T].name)

    def apply[T: Units](name: String, s: Sink[T, _]) = new Out[T](name, s.shape.in, Units[T].name)
    def apply[T: Units](name: String, s: SinkShape[T]) = new Out[T](name, s.in, Units[T].name)

    case class In[T: Units](name: String, let: Outlet[T], unit: String) extends Connection[T, Outlet]

    case class Out[T: Units](name: String, let: Inlet[T], unit: String) extends Connection[T, Inlet]

    implicit val letOut: Out ~> Inlet = Lambda[Out ~> Inlet](_.let)
    implicit val letIn: In ~> Outlet = Lambda[In ~> Outlet](_.let)
  }


  trait Connect[C[_], D[_]] {
    def apply(c: C[_], d: D[_]): Flw[_]
  }

  implicit val Cio: Connect[In, Out] = Flw(_, _)

  implicit val Coi: Connect[Out, In] = (c: Out[_], d: In[_]) => Flw(d.let, c.let)
}

