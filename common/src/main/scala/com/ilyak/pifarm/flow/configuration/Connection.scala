package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.GraphDSL
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

  def apply[T: Units](name: String, shape: SinkShape[T]) = new In[T](name, shape.in, Units[T].name)

  def apply[T: Units](name: String, shape: SourceShape[T]) = new Out[T](name, shape.out, Units[T].name)

  case class Out[T: Units](name: String, let: Outlet[T], unit: String) extends Connection[T, Outlet]

  case class In[T: Units](name: String, let: Inlet[T], unit: String) extends Connection[T, Inlet]

  implicit val letOut: Out ~> Outlet = Lambda[Out ~> Outlet](_.let)
  implicit val letIn: In ~> Inlet = Lambda[In ~> Inlet](_.let)

  object External {
    def apply[T: Units](name: String, s: SourceShape[T]) = new In[T](name, s.out, Units[T].name)

    def apply[T: Units](name: String, s: SinkShape[T]) = new Out[T](name, s.in, Units[T].name)

    case class In[T: Units](name: String, let: Outlet[T], unit: String) extends Connection[T, Outlet]

    case class Out[T: Units](name: String, let: Inlet[T], unit: String) extends Connection[T, Inlet]

    implicit val letOut: Out ~> Inlet = Lambda[Out ~> Inlet](_.let)
    implicit val letIn: In ~> Outlet = Lambda[In ~> Outlet](_.let)
  }


  trait Connect[C[_] <: TConnection[_], D[_] <: TConnection[_]] {
    def apply(c: C[_], d: D[_])(implicit b: GraphDSL.Builder[_]): Unit
  }

  implicit val Cio: Connect[In, Out] = new Connect[In, Out] {
    import GraphDSL.Implicits._
    override def apply(c: In[_], d: Out[_])(implicit b: GraphDSL.Builder[_]): Unit =
      d.let.as[Any] ~> c.let.as[Any]
  }

  implicit val Coi: Connect[Out, In] = new Connect[Out, In] {
    import GraphDSL.Implicits._
    override def apply(c: Out[_], d: In[_])(implicit b: GraphDSL.Builder[_]): Unit =
      c.let.as[Any] ~> d.let.as[Any]
  }
}

