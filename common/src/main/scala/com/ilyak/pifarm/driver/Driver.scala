package com.ilyak.pifarm.driver

import akka.actor.{ ActorRef, ActorSystem, PoisonPill, Props }
import akka.stream.scaladsl.{ Flow, GraphDSL, Keep, Merge, RestartFlow, RunnableGraph, Sink, Source }
import akka.stream.{ ActorMaterializer, ClosedShape, KillSwitches, OverflowStrategy }
import com.ilyak.pifarm.Port
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.driver.Driver.Connections
import com.ilyak.pifarm.flow.BroadcastActor.Receiver
import com.ilyak.pifarm.flow.{ ActorSink, BroadcastActor, SpreadToActors }

import scala.collection.immutable.Iterable
import scala.concurrent.duration.FiniteDuration

trait Driver {
  type Data
  val inputs: List[String]
  val outputs: List[String]

  val spread: PartialFunction[Data, String]

  def flow(port: Port, name: String): Flow[String, Data, _]

  def getPort(deviceId: String): Port

  def startActors(names: List[String])
                 (implicit system: ActorSystem): SMap[ActorRef] =
    startActors(names.map(n => n -> BroadcastActor(n)).toMap)

  def startActors(names: SMap[Props])
                 (implicit system: ActorSystem): SMap[ActorRef] =
    names.map {
      case (k, v) => k -> system.actorOf(v, k)
    }

  def connect(deviceId: String)
             (implicit s: ActorSystem,
              mat: ActorMaterializer): Either[String, Connections] = {
    val port = getPort(deviceId)
    val name = port.name
    val ins = startActors(inputs)
    val outs = startActors(outputs)
    val ff = flow(port, name)
      .viaMat(KillSwitches.single)(Keep.right)

    val graph = RunnableGraph.fromGraph(GraphDSL.create(ff) { implicit builder => f =>
        import GraphDSL.Implicits._
        val merge = builder.add(Merge[Any](ins.size, eagerComplete = false))
        ins.map {
          case (_, v) => Source.actorRef(1, OverflowStrategy.dropHead)
            .mapMaterializedValue(a => { v ! BroadcastActor.Receiver(a); a })
        }
          .map(builder.add(_))
          .foreach { _ ~> merge }

        val toStr = builder add Flow[Any].map(_.toString)
        val o = builder add SpreadToActors[Data](spread, outs)

        merge ~> toStr ~> f ~> o

        ClosedShape
    })

    val killSwitch = graph.run()
    val kill: () => Unit = () => {
      killSwitch.shutdown()
      val poison: SMap[ActorRef] => Unit = _.foreach{ _._2 ! PoisonPill }
      poison(ins)
      poison(outs)
    }

    Right(Connections(name, kill, ins, outs))
  }

  def parse(id: String)(msg: String): Iterable[Data]
}


object Driver {

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
  }

}
