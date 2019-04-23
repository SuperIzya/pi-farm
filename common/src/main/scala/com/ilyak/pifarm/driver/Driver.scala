package com.ilyak.pifarm.driver

import akka.actor.{ ActorRef, ActorSystem, PoisonPill, Props }
import akka.event.Logging
import akka.stream.scaladsl.{ Flow, GraphDSL, Keep, Merge, RestartFlow, RunnableGraph, Sink, Source }
import akka.stream._
import com.ilyak.pifarm.{ Decoder, Encoder, Port }
import com.ilyak.pifarm.Result.{ Err, Res }
import com.ilyak.pifarm.Types.{ Result, SMap }
import com.ilyak.pifarm.arduino.ArduinoActor
import com.ilyak.pifarm.driver.Driver.Connections
import com.ilyak.pifarm.flow.BroadcastActor.Receiver
import com.ilyak.pifarm.flow.{ ActorSink, BroadcastActor, SpreadToActors }

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration

trait Driver[TCommand, TData] {
  val inputs: List[String]
  val outputs: List[String]

  val spread: PartialFunction[TData, String]

  def flow(port: Port, name: String): Flow[String, String, _]

  def getPort(deviceId: String): Port

  def startActors(names: List[String])
                 (implicit system: ActorSystem): SMap[ActorRef] =
    startActors(names.map(n => n -> BroadcastActor.props(n)).toMap)

  def startActors(names: List[String], props: Props)(implicit s: ActorSystem): SMap[ActorRef] =
    startActors(names.map(n => n -> props).toMap)

  def startActors(names: SMap[Props])
                 (implicit system: ActorSystem): SMap[ActorRef] =
    names.map {
      case (k, v) => k -> system.actorOf(v, k)
    }

  def connect[C <: TCommand : Encoder, D <: TData : Decoder](
    deviceId: String
  )(
    implicit s: ActorSystem,
    mat: ActorMaterializer
  ): Result[Connections] = {
    val c = wrapConnect(f => f)
    c(deviceId)
  }


  def wrapConnect[C <: TCommand : Encoder, D <: TData : Decoder](
    wrap: FlowShape[String, String] => FlowShape[String, String]
  )(
    implicit s: ActorSystem,
    mat: ActorMaterializer
  ): String => Result[Connections] = deviceId => {
    val port = getPort(deviceId)
    val name = port.name
    val ins = startActors(inputs, ArduinoActor.props())
    val outs = startActors(outputs)
    val ff = flow(port, name)
      .mapConcat(x => immutable.Seq.empty ++ Decoder[D].decode(x))
      .viaMat(KillSwitches.single)(Keep.right)
    try {
      val graph = RunnableGraph.fromGraph(GraphDSL.create(ff) { implicit builder =>
        f =>
          import GraphDSL.Implicits._
          val merge = builder.add(Merge[C](ins.size, eagerComplete = false))
          ins.map {
            case (_, v) => Source.actorRef(1, OverflowStrategy.dropHead)
              .mapMaterializedValue(a => { v ! BroadcastActor.Receiver(a); a })
          }
            .map(builder.add(_))
            .foreach { _ ~> merge }

          val toStr = builder add Flow[C].map(Encoder[C].encode)
          val o = builder add SpreadToActors[D](spread, outs)

          merge ~> toStr ~> f ~> o

          ClosedShape
      })

      val killSwitch = graph.run()
      val kill: () => Unit = () => {
        killSwitch.shutdown()
        val poison: SMap[ActorRef] => Unit = _.foreach { _._2 ! PoisonPill }
        poison(ins)
        poison(outs)
      }

      Res(Connections(name, kill, ins, outs))
    }
    catch {
      case e: Exception => Err(e.getMessage)
    }
  }
}


object Driver {

  type Connector = String => Result[Connections]

  case class Meta(
    inputs: Seq[String],
    outputs: Seq[String],
    name: String,
    binPath: String
  )

  case class Connections(id: String,
                         killSwitch: () => Unit,
                         inputs: Map[String, ActorRef],
                         outputs: Map[String, ActorRef])

  def source[T](producer: ActorRef): Source[T, _] = Source
    .actorRef(1, OverflowStrategy.dropHead)
    .mapMaterializedValue(actor => {
      producer ! Receiver(actor)
      actor
    })

  def sink[T](consumer: ActorRef): Sink[T, _] = Flow[T].to(ActorSink[T](consumer))


  trait DriverFlow {

    def restartFlow[In, Out](minBackoff: FiniteDuration,
                             maxBackoff: FiniteDuration): (() ⇒ Flow[In, Out, _]) => Flow[In, Out, _] =
      RestartFlow.withBackoff[In, Out](
        minBackoff,
        maxBackoff,
        randomFactor = 0.2
      )

    def logSink(name: String): Sink[String, _] =
      Flow[String]
        .log(name)
        .withAttributes(Attributes.logLevels(
          onFailure = Logging.ErrorLevel,
          onFinish = Logging.WarningLevel,
          onElement = Logging.WarningLevel
        ))
        .to(Sink.ignore)
  }

}
