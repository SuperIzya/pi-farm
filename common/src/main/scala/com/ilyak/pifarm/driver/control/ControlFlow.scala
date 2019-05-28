package com.ilyak.pifarm.driver.control

import akka.actor.{ ActorSystem, PoisonPill }
import akka.event.Logging
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.stream.{ Attributes, OverflowStrategy }
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types.Result
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo }
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration }
import com.ilyak.pifarm.{ Result, RunInfo }
import play.api.libs.json.{ Json, OFormat }

import scala.language.postfixOps

class ControlFlow(system: ActorSystem,
                  metaData: MetaData,
                  runInfo: RunInfo)
  extends FlowAutomaton[ButtonEvent, LedCommand] {

  override def flow(conf: Configuration.Node): Result[Flow[ButtonEvent, LedCommand, _]] = {
    val actor = runInfo.deviceActor
    val controlActor = system.actorOf(ControlActor.props(actor, runInfo))

    val sink = Flow[ButtonEvent]
      .log(ControlFlow.name)
      .withAttributes(Attributes.logLevels(
        onFailure = Logging.ErrorLevel,
        onFinish = Logging.WarningLevel,
        onElement = Logging.WarningLevel
      ))
      .to(Sink.actorRef(controlActor, PoisonPill))

    val source = Source.actorRef[LedCommand](10, OverflowStrategy.dropHead)
      .mapMaterializedValue { a =>
        controlActor ! Subscribe(a)
        a
      }

    Result.Res(
      Flow.fromSinkAndSourceCoupled(sink, source)
    )
  }
}

object ControlFlow {
  val name = "default-control-flow"

  case class Params(device: String)

  implicit val paramsFormat: OFormat[Params] = Json.format

  def apply(parserInfo: MetaParserInfo): ControlFlow = {
    new ControlFlow(parserInfo.systemImplicits.actorSystem, parserInfo.metaData, parserInfo.runInfo)
  }

  val configuration: Configuration.Graph = Configuration.Graph(
    name,
    Seq(
      Configuration.Node(
        name,
        List("the-button"),
        List("the-led"),
        meta = MetaData(
          Some(name),
          None,
          BlockType.Automaton,
          plugin = "default-control",
          blockName = name,
          params = ""
        )
      )
    ),
    List("the-button"),
    List("the-led"),
    Map.empty
  )
}
