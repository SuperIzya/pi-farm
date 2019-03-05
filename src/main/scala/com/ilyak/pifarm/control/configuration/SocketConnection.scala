package com.ilyak.pifarm.control.configuration

import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import akka.stream.{Inlet, Outlet, SinkShape, SourceShape}
import com.ilyak.pifarm.flow.Messages.Data

sealed trait ConnectionSocket
object SocketConnection {


  trait Input extends ConnectionSocket {
    def in: Inlet[Data]
  }

  trait Output extends ConnectionSocket {
    def out: Outlet[Data]
  }


  implicit class inputConnector(val in: SourceShape[Data]) extends AnyVal {
    import GraphDSL.Implicits._
    def connect(output: Sink[Data, _])(implicit builder: GraphDSL.Builder[_]): Unit = {
      in ~> output
    }

    def connect(output: SinkShape[Data])(implicit builder: GraphDSL.Builder[_]): Unit = {
      in ~> output
    }
  }

  implicit class outputConnector(val out: SinkShape[Data]) extends AnyVal {
    import GraphDSL.Implicits._
    def connect(input: Source[Data, _])(implicit builder: GraphDSL.Builder[_]): Unit = {
      input ~> out
    }

    def connect(input: SourceShape[Data])(implicit builder: GraphDSL.Builder[_]): Unit = {
      input ~> out
    }
  }


}