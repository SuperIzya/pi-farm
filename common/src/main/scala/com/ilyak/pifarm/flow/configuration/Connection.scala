package com.ilyak.pifarm.flow.configuration

import akka.stream._
import akka.stream.scaladsl.GraphDSL
import cats.Monoid
import com.ilyak.pifarm.State.GraphState
import com.ilyak.pifarm.Types._
import com.ilyak.pifarm.flow.configuration.Connection.{ Sockets, TConnection }
import com.ilyak.pifarm.{ BuildResult, Units }

import scala.language.higherKinds


sealed trait Connection[T, L[_]] extends TConnection {
  type Let = L[T]
  type GetLet = GraphState => (GraphState, Let)

  val let: GetLet
}

object Connection {

  sealed trait TConnection {
    val unit: String
    val node: String
    val name: String
  }

  type ConnectShape = GRun[Unit]

  case class Sockets(inputs: Map[String, Inlet[_]], outputs: Map[String, Outlet[_]])

  object Sockets {
    val empty = new Sockets(Map.empty, Map.empty)
  }


  object ConnectShape {
    val empty: ConnectShape = s => _ => (s, Unit)

    private def tryConnect[C <: TConnection, D <: TConnection]
    (x: C, y: D, connect: GRun[Unit]): BuildResult[ConnectShape] = {
      BuildResult.cond(
        x.unit == y.unit,
        connect,
        s"Wrong units (${ x.name }:${ x.unit } -> ${ y.name }:${ y.unit })"
      )
    }

    def apply(in: In[_], out: Out[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      import com.ilyak.pifarm.State.Implicits._
      val run: GRun[Unit] = ss => implicit b => {
        val (s1, sOut) = ss(out.node, out.let(_))
        val (s2, sIn) = s1(in.node, in.let(_))
        sOut.as[Any] ~> sIn.as[Any]
        (s2, Unit)
      }
      tryConnect(out, in, run)
    }

    def apply(out: Out[_], in: In[_]): BuildResult[ConnectShape] = apply(in, out)

    def apply(in: In[_], extIn: External.In[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      import com.ilyak.pifarm.State.Implicits._
      tryConnect(in, extIn, ss => implicit b => {
        val (s1, sOut) = ss(extIn.node, extIn.let(_))
        val (s2, sIn) = s1(in.node, in.let(_))
        sOut.as[Any] ~> sIn.as[Any]
        (s2, Unit)
      })
    }

    def apply(out: Out[_], extOut: External.Out[_]): BuildResult[ConnectShape] = {
      import GraphDSL.Implicits._
      import com.ilyak.pifarm.State.Implicits._
      tryConnect(out, extOut, ss => implicit b => {
        val (s1, sOut) = ss(out.node, out.let(_))
        val (s2, sIn) = s1(extOut.node, extOut.let(_))
        sOut.as[Any] ~> sIn.as[Any]
        (s2, Unit)
      })
    }

    implicit val monad: Monoid[ConnectShape] = new Monoid[ConnectShape] {
      override def empty: ConnectShape = ConnectShape.empty

      override def combine(x: ConnectShape, y: ConnectShape): ConnectShape = (x, y) match {
        case (ConnectShape.empty, ConnectShape.empty) => ConnectShape.empty
        case (ConnectShape.empty, a) => a
        case (a, ConnectShape.empty) => a
        case (c, d) =>
          val res: ConnectShape = ss => b => {
            val (s1, _) = c(ss)(b)
            val (s2, _) = d(s1)(b)
            (s2, Unit)
          }
          res
      }
    }
  }


  object In {
    def apply[T: Units](name: String, node: String): In[T] =
      apply(name, node, _.inputs(name).as[T])
    def apply[T: Units](name: String, node: String, shape: In[T]#GetLet): In[T] =
      new In(name, node, Units[T].name, shape(_))
  }

  object Out {
    def apply[T: Units](name: String, node: String): Out[T] =
      apply(name, node, _.outputs(name).as[T])
    def apply[T: Units](name: String, node: String, shape: Out[T]#GetLet): Out[T] =
      new Out(name, node, Units[T].name, shape(_))
  }

  case class Out[T] private(name: String,
                            node: String,
                            unit: String,
                            let: Out[T]#GetLet)
    extends Connection[T, Outlet]

  case class In[T] private(name: String,
                           node: String,
                           unit: String,
                           let: In[T]#GetLet)
    extends Connection[T, Inlet]

  object External {

    object In {
      def apply[T: Units](name: String, node: String): In[T] =
        apply(name, node, _.outputs(name).as[T])
      def apply[T: Units](name: String, node: String, add: In[T]#GetLet): In[T] =
        new In(name, node, Units[T].name, add(_))
    }

    object Out {
      def apply[T: Units](name: String, node: String): Out[T] =
        apply(name, node, _.inputs(name).as[T])
      def apply[T: Units](name: String, node: String, add: Out[T]#GetLet): Out[T] =
        new Out[T](name, node, Units[T].name, add)
    }

    case class In[T] private(name: String,
                             node: String,
                             unit: String,
                             let: In[T]#GetLet)
      extends Connection[T, Outlet]

    case class Out[T] private(name: String,
                              node: String,
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

