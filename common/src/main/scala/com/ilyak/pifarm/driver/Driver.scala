package com.ilyak.pifarm.driver

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }
import akka.event.Logging
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl.{ Flow, GraphDSL, Keep, Merge, RestartFlow, RunnableGraph, Sink, Source }
import akka.util.Timeout
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Decoder.DecoderShape
import com.ilyak.pifarm.Decoder.Trie.PrefixForest
import com.ilyak.pifarm.Result.{ Err, Res }
import com.ilyak.pifarm.Types.{ Result, SMap, WrapFlow }
import com.ilyak.pifarm._
import com.ilyak.pifarm.arduino.ArduinoActor
import com.ilyak.pifarm.driver.Driver.KillActor.Kill
import com.ilyak.pifarm.driver.Driver.{ Connector, InStarter, KillActor, OutStarter, RunningDriver }
import com.ilyak.pifarm.flow.configuration.{ Configuration, Connection => Conn }
import com.ilyak.pifarm.flow.{ ActorSink, SpreadToActors }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Await, ExecutionContext }
import scala.language.{ higherKinds, implicitConversions, postfixOps }

trait Driver {
  val inputs: SMap[InStarter[_]]
  val outputs: SMap[OutStarter[_]]

  val ignoreDuplicateDecoders: Boolean = false

  lazy val encoders: Encoder[Any] = Encoder.merge(inputs.values.map(_.encoder))

  lazy val decoders: PrefixForest =
    Decoder.merge(outputs.values.map(_.decoder), ignoreDuplicateDecoders) match {
      case Result.Err(e) => throw new Exception(s"Failed to merge deciders due to $e")
      case Result.Res(v) => v
    }

  val initialCommands: List[String] = List.empty
  val tokenSeparator: String

  val companion: DriverCompanion[_ <: Driver]

  val spread: PartialFunction[Any, String]

  def flow(port: Port, name: String, wrapFlow: WrapFlow)(implicit ex: ExecutionContext): Flow[String, String, _]

  def getPort(deviceId: String): Port

  implicit def cmdToString[T: Encoder](cmd: T): String = Encoder[T].encode(cmd)

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

  def connector(deviceProps: Props)
               (implicit s: ActorSystem,
                mat: ActorMaterializer): Connector = {
    val encode = encoders.encode
    val decode = decoders
    Connector(companion.name, (deviceId, connector) => {
      import s.dispatcher

      // TODO: Process failure to open port
      val port = getPort(deviceId)
      val name = port.name
      val ins: SMap[ActorRef] = startActors(inputs.keySet, deviceId, ArduinoActor.props())
      val outs: SMap[ActorRef] = startActors(outputs.keySet, deviceId)

      def conv[T[_]](actors: SMap[ActorRef], creators: SMap[ActorRef => T[_]]): SMap[T[_]] =
        actors.map { case (k, v) => k -> creators(k)(v) }.toMap

      val extIns = conv(ins, inputs.mapValues(_.start))
      val extOuts = conv(outs, outputs.mapValues(_.start))

      val wrappedFlow  = flow(port, name, connector.wrap).viaMat(KillSwitches.single)(Keep.right)
      val deviceActor = s.actorOf(deviceProps, s"device-${ deviceId.hashCode }")
      try {
        val graph = RunnableGraph.fromGraph(GraphDSL.create(wrappedFlow) { implicit builder =>
          extFlow =>
            import GraphDSL.Implicits._
            val mergeInputs = builder.add(Merge[Any](ins.size, eagerComplete = false))
            ins.map {
              case (_, v) => Source.actorRef(1, OverflowStrategy.dropHead)
                .mapMaterializedValue(a => { v ! BroadcastActor.Producer(a); a })
            }
              .map(builder.add(_))
              .foreach { _ ~> mergeInputs }

            val toStr = builder add Flow[Any].map(encode)
            val fromStr = builder add Flow[String]
              .mapConcat(s => s.split(tokenSeparator).toList)
              .via(new DecoderShape(decode))
              .mapConcat(l => l)

            val initial = Flow[String].merge(Source.fromIterator(() => initialCommands.toIterator))

            val spreadEP = builder add SpreadToActors(spread, outs)

            mergeInputs ~> toStr ~> initial ~> extFlow ~> fromStr ~> spreadEP

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
          s.stop(deviceActor)
        }

        Res(RunningDriver(name, kill, deviceActor, companion.defaultConfigurations, extIns, extOuts))
      }
      catch {
        case e: Exception => Err(e.getMessage)
      }
    })
  }
}


object Driver {

  private val IdWrap: WrapFlow = x => x

  case class OutStarter[+T] private(decoder: Decoder[_ <: T], start: ActorRef => Conn.External.ExtOut[_ <: T])

  object OutStarter {
    def apply[T: Decoder : Units](name: String, nodeName: String): OutStarter[T] =
      new OutStarter(Decoder[T], Conn.External.ExtOut[T](name, nodeName, _))

    def apply[T: Decoder, P <: T](start: ActorRef => Conn.External.ExtOut[P]): OutStarter[T] =
      new OutStarter[T](Decoder[T], start)
  }

  case class InStarter[+T] private(encoder: Encoder[_ <: T], start: ActorRef => Conn.External.ExtIn[_ <: T])

  object InStarter {
    def apply[T: Encoder : Units](name: String, nodeName: String): InStarter[T] =
      new InStarter[T](Encoder[T], Conn.External.ExtIn[T](name, nodeName, _))

    def apply[T: Encoder](start: ActorRef => Conn.External.ExtIn[_ <: T]): InStarter[T] =
      new InStarter[T](Encoder[T], start)
  }

  case class Connector(name: String,
                       f: (String, Connector) => Result[RunningDriver],
                       wrap: WrapFlow = IdWrap)

  implicit class ConnectorOps(val connector: Connector) extends AnyVal {
    def connect(device: String): Result[RunningDriver] = connector.f(device, connector)

    def wrapFlow(wrap: WrapFlow): Connector =
      connector.copy(wrap = if (connector.wrap == IdWrap) wrap else wrap.andThen(connector.wrap))
  }

  case class Meta(
    inputs: Seq[String],
    outputs: Seq[String],
    name: String,
    binPath: String
  )

  /**
    *
    * Running driver
    *
    * @param id                    - Id of the device
    * @param kill                  - kill switch to stop the driver
    * @param deviceActor           - actor to receive all input addressed to device
    * @param defaultConfigurations - [[List]] of default [[Configuration.Graph]]s' for this driver
    * @param inputs                - inputs expected by driver
    * @param outputs               - outputs provided by driver
    */
  case class RunningDriver(id: String,
                           kill: () => Unit,
                           deviceActor: ActorRef,
                           defaultConfigurations: List[Configuration.Graph],
                           inputs: SMap[Conn.External.ExtIn[_]],
                           outputs: SMap[Conn.External.ExtOut[_]])

  def source[T](producer: ActorRef): Source[T, _] = Source
    .actorRef(1, OverflowStrategy.dropHead)
    .mapMaterializedValue(actor => {
      producer ! Producer(actor)
      actor
    })

  def sink[T](consumer: ActorRef): Sink[T, _] = Flow[T].to(ActorSink[T](consumer))


  object Implicits {

    implicit class FlowOps(val flow: Flow[String, String, _]) extends AnyVal {
      def logPi(name: String): Flow[String, String, _] =
        flow.log(name).withAttributes(Attributes.logLevels(
          onFailure = Logging.ErrorLevel,
          onFinish = Logging.WarningLevel,
          onElement = Logging.WarningLevel
        ))

      def distinct: Flow[String, String, _] =
        flow.statefulMapConcat(() => {
          var lastVal: String = ""
          str => {
            if (str == lastVal) List.empty[String]
            else {
              lastVal = str
              List(str)
            }
          }
        })
    }

  }

  trait DriverFlow {

    import Implicits._

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
        .logPi(name)
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
