package com.ilyak.pifarm.driver.control

import akka.actor.{ ActorSystem, PoisonPill }
import akka.event.Logging
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.stream.{ Attributes, OverflowStrategy }
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types.{ GBuilder, Result }
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.ConfigurableAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo }
import com.ilyak.pifarm.flow.configuration.Connection.Sockets
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration, Connection }
import com.ilyak.pifarm.{ Result, RunInfo }
import play.api.libs.json.{ Json, OFormat }

import scala.language.postfixOps

class ControlFlow(system: ActorSystem,
                  metaData: MetaData,
                  runInfo: RunInfo)
  extends ConfigurableAutomaton {

  override def inputs(node: Configuration.Node): Result[Seq[Connection.In[_]]] =
    Result(Seq(
      Connection.In[ButtonEvent]("the-button", node.id)
    ))

  override def outputs(node: Configuration.Node): Result[Seq[Connection.Out[_]]] =
    Result(Seq(
      Connection.Out[LedCommand]("the-led", node.id),
      Connection.Out[ResetCommand.type]("reset", node.id)
    ))

  override def buildShape(node: Configuration.Node): Result[GBuilder[Connection.Sockets]] = Result.Res {
    b => {
      val actor = runInfo.deviceActor
      val controlActor = system.actorOf(ControlActor.props(actor, runInfo))

      val sink = b add Flow[ButtonEvent]
        .log(ControlFlow.name)
        .withAttributes(Attributes.logLevels(
          onFailure = Logging.ErrorLevel,
          onFinish = Logging.WarningLevel,
          onElement = Logging.WarningLevel
        ))
        .to(Sink.actorRef(controlActor, PoisonPill))

      val srcLed = b add Source.actorRef[LedCommand](10, OverflowStrategy.dropHead)
        .mapMaterializedValue { a =>
          controlActor ! Subscribe(a)
          a
        }

      val srcReset = b add Source.actorRef[ResetCommand.type](10, OverflowStrategy.dropHead)
        .mapMaterializedValue { a =>
          controlActor ! Subscribe(a)
          a
        }

      Sockets(
        Map("the-button" -> sink.in),
        Map(
          "the-led" -> srcLed.out,
          "reset" -> srcReset.out
        )
      )
    }
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
        List("the-led", "reset"),
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
    List("the-led", "reset"),
    Map.empty
  )
}
