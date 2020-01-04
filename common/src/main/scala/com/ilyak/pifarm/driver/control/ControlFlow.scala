package com.ilyak.pifarm.driver.control

import akka.actor.{ActorSystem, PoisonPill}
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Attributes, OverflowStrategy}
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.RunInfo
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{
  ConfigurableAutomaton,
  NodeCompanion,
  XLet
}
import com.ilyak.pifarm.flow.configuration.Configuration.{
  MetaData,
  MetaParserInfo,
  ParseMeta
}
import com.ilyak.pifarm.flow.configuration.Connection.Sockets
import com.ilyak.pifarm.flow.configuration.{
  BlockType,
  ConfigurableNode,
  Configuration,
  Connection
}
import com.ilyak.pifarm.types.{GBuilder, Result}
import play.api.libs.json.{Json, OFormat}

import scala.language.postfixOps

class ControlFlow(system: ActorSystem, metaData: MetaData, runInfo: RunInfo)
    extends ConfigurableAutomaton {

  override def inputs(node: Configuration.Node): Result[Seq[Connection.In[_]]] =
    Result(Seq(Connection.In[ButtonEvent]("the-button", node.id)))

  override def outputs(
    node: Configuration.Node
  ): Result[Seq[Connection.Out[_]]] =
    Result(
      Seq(
        Connection.Out[LedCommand]("the-led", node.id),
        Connection.Out[ResetCommand.type]("reset", node.id)
      )
    )

  override def buildShape(
    node: Configuration.Node
  ): Result[GBuilder[Connection.Sockets]] = Result.Res { b =>
    {
      val actor = runInfo.deviceActor
      val controlActor = system.actorOf(ControlActor.props(actor, runInfo))

      val sink = b add Flow[ButtonEvent]
        .log(ControlFlow.name)
        .withAttributes(
          Attributes.logLevels(
            onFailure = Logging.ErrorLevel,
            onFinish = Logging.WarningLevel,
            onElement = Logging.DebugLevel
          )
        )
        .to(Sink.actorRef(controlActor, PoisonPill))

      val srcLed = b add Source
        .actorRef[LedCommand](10, OverflowStrategy.dropHead)
        .mapMaterializedValue { a =>
          controlActor ! Subscribe(a)
          a
        }

      val srcReset = b add Source
        .actorRef[ResetCommand.type](10, OverflowStrategy.dropHead)
        .mapMaterializedValue { a =>
          controlActor ! Subscribe(a)
          a
        }

      Sockets(
        Map("the-button" -> sink.in),
        Map("the-led" -> srcLed.out, "reset" -> srcReset.out)
      )
    }
  }
}

object ControlFlow {
  case class Params(device: String)

  implicit val paramsFormat: OFormat[Params] = Json.format

  def apply(parserInfo: MetaParserInfo): ControlFlow = {
    new ControlFlow(
      parserInfo.systemImplicits.actorSystem,
      parserInfo.metaData,
      parserInfo.runInfo
    )
  }

  implicit val companion: NodeCompanion[ControlFlow] =
    new NodeCompanion[ControlFlow] {
      override val inputs: List[ConfigurableNode.XLet] =
        List(XLet[ButtonEvent]("the-button"))
      override val outputs: List[ConfigurableNode.XLet] =
        List(XLet[LedCommand]("the-led"), XLet[ResetCommand.type]("reset"))

      override val blockType: BlockType = BlockType.Automaton
      override val name = "default-control-flow"
      override val creator: ParseMeta[ControlFlow] = ControlFlow(_)
    }

  val name = companion.name

  val configuration: Configuration.Graph = Configuration.Graph(
    companion.name,
    Seq(
      Configuration.Node(
        companion.name,
        companion.inputNames,
        companion.outputNames,
        meta = MetaData(
          Some(companion.name),
          None,
          companion.blockType,
          plugin = "default-control",
          blockName = companion.name,
          params = ""
        )
      )
    ),
    companion.inputNames,
    companion.outputNames,
    Map.empty
  )
}
