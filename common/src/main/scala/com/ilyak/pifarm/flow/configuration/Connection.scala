package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import cats.{Eval, Monoid}
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.flow.configuration.Connection.TConnection
import com.ilyak.pifarm.{BuildResult, Units}

import scala.language.higherKinds


sealed trait Connection[T, L[_]] extends TConnection {
  type Let = L[T]
  type GetLet = GraphDSL.Builder[_] => Eval[Let]

  val let: GetLet

}

object Connection {

  sealed trait TConnection {
    val unit: String
    val name: String
  }

  type GraphBuilder = GraphDSL.Builder[_]
  type GBuilder[T] = GraphDSL.Builder[_] => Eval[T]
  type ConnectShape = GBuilder[Unit]

  type AddShape = GBuilder[Sockets]

  case class Sockets(inputs: Map[String, Inlet[_]], outputs: Map[String, Outlet[_]])


  object ConnectShape {
    val empty: ConnectShape = _ => Eval.now(Unit)

    private def tryConnect[C <: TConnection, D <: TConnection]
    (x: C, y: D, connect: => ConnectShape): BuildResult[ConnectShape] = {
      BuildResult.cond(
        x.unit == y.unit,
        connect,
        s"Wrong units (${x.name}:${x.unit} -> ${y.name}:${y.unit})"
      )
    }

    def apply(in: In[_], out: Out[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      tryConnect(out, in, implicit b => Eval.now{
        val sOut = out.let(b).value
        val sIn = in.let(b).value
        sOut.as[Any] ~> sIn.as[Any]
      })
    }

    def apply(out: Out[_], in: In[_]): BuildResult[ConnectShape] = apply(in, out)

    def apply(in: In[_], extIn: External.In[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      tryConnect(in, extIn, implicit b => Eval.now{
        val sOut = extIn.let(b).value
        val sIn = in.let(b).value
        sOut.as[Any] ~> sIn.as[Any]
      })
    }

    def apply(out: Out[_], extOut: External.Out[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      tryConnect(out, extOut, implicit b => Eval.now{
        val sOut = out.let(b).value
        val sIn = extOut.let(b).value

        sOut.as[Any] ~> sIn.as[Any]
      })
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

  object In {
    def apply[T: Units](name: String, shape: SinkShape[T]): In[T] = {
      val addShape: GBuilder[Inlet[T]] = _ => Eval.now(shape).map(_.in)
      apply(name, addShape)
    }

    def apply[T: Units](name: String, flow: Sink[T, _]): In[T] = {
      val addShape: GBuilder[Inlet[T]] = b => Eval.now(b add flow).map(_.in)
      apply(name, addShape)
    }

    def apply[T: Units](name: String, shape: GBuilder[Inlet[T]]): In[T] =
      new In(name, Units[T].name, shape(_))
  }

  object Out {

    def apply[T: Units](name: String, src: Source[T, _]): Out[T] = {
      val addShape: GBuilder[Outlet[T]] = b => Eval.now(b add src).map(_.out)
      apply(name, addShape)
    }

    def apply[T: Units](name: String, shape: SourceShape[T]): Out[T] = {
      val addShape: GBuilder[Outlet[T]] = _ => Eval.now(shape).map(_.out)
      apply(name, addShape)
    }

    def apply[T: Units](name: String, shape: GBuilder[Outlet[T]]): Out[T] =
      new Out(name, Units[T].name, shape(_))
  }

  case class Out[T] private(name: String,
                            unit: String,
                            let: Out[T]#GetLet)
    extends Connection[T, Outlet]

  case class In[T] private(name: String,
                           unit: String,
                           let: In[T]#GetLet)
    extends Connection[T, Inlet]

  object External {

    object In {
      def apply[T: Units](name: String, add: GBuilder[Outlet[T]]): In[T] =
        new In(name, Units[T].name, add(_))


      def apply[T: Units](name: String, src: Source[T, _]): In[T] = {
        val addShape: GBuilder[Outlet[T]] = b => Eval.now(b add src).map(_.out)
        apply(name, addShape)
      }

      def apply[T: Units](name: String, s: SourceShape[T]): In[T] = {
        val addShape: GBuilder[Outlet[T]] = _ => Eval.now(s).map(_.out)
        apply(name, addShape)
      }
    }

    object Out {
      def apply[T: Units](name: String, add: GBuilder[Inlet[T]]): Out[T] =
        new Out[T](name, Units[T].name, add(_))

      def apply[T: Units](name: String, s: Sink[T, _]): Out[T] = {
        val addShape: GBuilder[Inlet[T]] = b => Eval.now(b add s).map(_.in)
        apply(name, addShape)
      }

      def apply[T: Units](name: String, s: SinkShape[T]): Out[T] = {
        val addShape: GBuilder[Inlet[T]] = _ => Eval.now(s).map(_.in)
        apply(name, addShape)
      }
    }

    case class In[T] private(name: String,
                             unit: String,
                             let: In[T]#GetLet)
      extends Connection[T, Outlet]

    case class Out[T] private(name: String,
                              unit: String,
                              let: Out[T]#GetLet)
      extends Connection[T, Inlet]

  }


  trait ConnectF[C[_], D[_]] {
    def apply(c: C[_], d: D[_]): BuildResult[ConnectShape]
  }

  implicit val Cio: ConnectF[In, Out] = ConnectShape(_, _)
  implicit val Coi: ConnectF[Out, In] = ConnectShape(_, _)
  implicit val Ceio: ConnectF[In, External.In] = ConnectShape(_, _)
  implicit val Ceoi: ConnectF[Out, External.Out] = ConnectShape(_, _)
}

