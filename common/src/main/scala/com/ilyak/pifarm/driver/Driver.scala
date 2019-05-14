package com.ilyak.pifarm.driver

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }
import akka.event.Logging
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl.{ Flow, GraphDSL, Keep, Merge, RestartFlow, RunnableGraph, Sink, Source }
import akka.util.Timeout
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Result.{ Err, Res }
import com.ilyak.pifarm.Types.{ Result, SMap, WrapFlow }
import com.ilyak.pifarm.arduino.ArduinoActor
import com.ilyak.pifarm.flow.configuration.{ Connection => Conn }
import com.ilyak.pifarm.driver.Driver.KillActor.Kill
import com.ilyak.pifarm.driver.Driver.{ Connections, Connector, KillActor }
import com.ilyak.pifarm.flow.{ ActorSink, SpreadToActors }
import com.ilyak.pifarm.{ BroadcastActor, Decoder, Encoder, Port }

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.language.{ higherKinds, postfixOps }

trait Driver[TCommand, TData] {
  val inputs: SMap[ActorRef => Conn.External.In[_ <: TCommand]]
  val outputs: SMap[ActorRef => Conn.External.Out[_ <: TData]]

  val spread: PartialFunction[TData, String]

  def flow(port: Port, name: String): Flow[String, String, _]

  def getPort(deviceId: String): Port

  def startActors(names: Set[String],
                  deviceId: String)
                 (implicit system: ActorSystem): SMap[ActorRef] =
    startActors(names.map(n => n -> BroadcastActor.props(n)).toMap, deviceId)

  def startActors(names: Set[String],
                  deviceId: String,
                  props: Props)
                 (implicit s: ActorSystem): SMap[ActorRef] =
    startActors(names.map(n => n -> props).toMap, deviceId)

  def startActors(names: SMap[Props], deviceId: String)
                 (implicit system: ActorSystem): SMap[ActorRef] =
    names.map {
      case (k, v) => k -> system.actorOf(v, s"$k-${ deviceId.hashCode() }")
    }

  def connector[C <: TCommand : Encoder, D <: TData : Decoder]
    (implicit s: ActorSystem,
     mat: ActorMaterializer): Connector =
    Connector((deviceId, connector) => {
      val port = getPort(deviceId)
      val name = port.name
      val ins: SMap[ActorRef] = startActors(inputs.keySet, deviceId, ArduinoActor.props())
      val outs: SMap[ActorRef] = startActors(outputs.keySet, deviceId)
      val ff = flow(port, name).viaMat(KillSwitches.single)(Keep.right)
      val wrappedFlow = connector.wrap(ff)
      try {
        val graph = RunnableGraph.fromGraph(GraphDSL.create(wrappedFlow) { implicit builder =>
          extFlow =>
            import GraphDSL.Implicits._
            val merge = builder.add(Merge[C](ins.size, eagerComplete = false))
            ins.map {
              case (_, v) => Source.actorRef(1, OverflowStrategy.dropHead)
                .mapMaterializedValue(a => { v ! BroadcastActor.Producer(a); a })
            }
              .map(builder.add(_))
              .foreach { _ ~> merge }

            val toStr = builder add Flow[C].map(Encoder[C].encode)
            val fromStr = builder add Flow[String]
              .mapConcat(s => immutable.Seq() ++ Decoder[D].decode(s))

            val o = builder add SpreadToActors[D](spread, outs)

            merge ~> toStr ~> extFlow ~> fromStr ~> o

            ClosedShape
        })

        val killSwitch = graph.run()
        val kill: () => Unit = () => {
          import s.dispatcher

          import scala.concurrent.duration._

          implicit val timeout: Timeout = Timeout(2 seconds)
          killSwitch.shutdown()
          val killActor = s.actorOf(KillActor.props())
          val future = (killActor ? Kill(ins.values.toList))
            .flatMap(_ => killActor ? Kill(outs.values.toList))
          Await.result(future, Duration.Inf)
          s.stop(killActor)
        }

        def conv[T[_]](actors: SMap[ActorRef], creators: SMap[ActorRef => T[_]]): SMap[T[_]] =
          actors.map { case (k, v) => k -> creators(k)(v) }.toMap

        val extIns = conv(ins, inputs)
        val extOuts = conv(outs, outputs)

        Res(Connections(name, kill, extIns, extOuts))
      }
      catch {
        case e: Exception => Err(e.getMessage)
      }
    })

}


object Driver {

  private val IdWrap: WrapFlow = x => x

  case class Connector(f: (String, Connector) => Result[Connections], wrap: WrapFlow = IdWrap)

  implicit class ConnectorOps(val connector: Connector) extends AnyVal {
    def connect(device: String): Result[Connections] = connector.f(device, connector)

    def wrapFlow(wrap: WrapFlow): Connector =
      connector.copy(wrap = if(connector.wrap == IdWrap) wrap else wrap.andThen(connector.wrap))
  }

  case class Meta(
    inputs: Seq[String],
    outputs: Seq[String],
    name: String,
    binPath: String
  )

  case class Connections(id: String,
                         killSwitch: () => Unit,
                         inputs: SMap[Conn.External.In[_]],
                         outputs: SMap[Conn.External.Out[_]])

  def source[T](producer: ActorRef): Source[T, _] = Source
    .actorRef(1, OverflowStrategy.dropHead)
    .mapMaterializedValue(actor => {
      producer ! Producer(actor)
      actor
    })

  def sink[T](consumer: ActorRef): Sink[T, _] = Flow[T].to(ActorSink[T](consumer))


  trait DriverFlow {

    def restartFlow[In, Out](minBackoff: FiniteDuration,
                             maxBackoff: FiniteDuration): (() ⇒ Flow[In, Out, _]) => Flow[In, Out, _] =
      RestartFlow.withBackoff[In, Out](
        minBackoff,
        maxBackoff,
        randomFactor = 0.2,
        3
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

  class KillActor extends Actor with ActorLogging {
    var count: Int = 0
    var waits: ActorRef = _

    override def receive: Receive = {
      case Kill(actors) =>
        waits = sender()
        count = actors.size
        actors foreach context.watch
        actors foreach context.stop
        log.debug(s"Terminating ${ actors.size } actors")
      case Terminated(a) =>
        log.debug(s"Terminated $a")
        context.unwatch(a)
        count -= 1
        if (count == 0) {
          waits ! 'done
          waits = null
        }
    }
  }

  object KillActor {
    def props(): Props = Props[KillActor]

    case class Kill(actors: List[ActorRef])

    case class TotalKill(actors: List[ActorRef])

  }

}
