package com.ilyak.pifarm.configuration.control

import akka.actor.{ ActorRef, ActorSystem, PoisonPill }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types.Result
import com.ilyak.pifarm.configuration.control.ControlFlow.Params
import com.ilyak.pifarm.driver.control.{ ButtonEvent, LedCommand }
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo }
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration }
import com.ilyak.pifarm.{ Result, RunInfo }
import play.api.libs.json.{ JsError, JsSuccess, Json, OFormat }

import scala.concurrent.Await
import scala.language.postfixOps

class ControlFlow(system: ActorSystem,
                  metaData: MetaData,
                  runInfo: RunInfo)
  extends FlowAutomaton[ButtonEvent, LedCommand] {

  import scala.concurrent.duration._

  override def flow(conf: Configuration.Node): Result[Flow[ButtonEvent, LedCommand, _]] =
    Json.fromJson[Params](Json.parse(metaData.params)) match {
      case JsError(errors) =>
        Result.Err(s"Failed to parse params out of node ${ conf.id } due to $errors")
      case JsSuccess(value, _) =>
        val d = 1 second
        val actor = Await.result(system.actorSelection(value.socket).resolveOne(d), d)
        val controlActor = system.actorOf(ControlActor.props(actor, runInfo))

        val sink = Sink.actorRef[ButtonEvent](controlActor, PoisonPill)
        val source = Source.actorRef[LedCommand](1, OverflowStrategy.dropHead)
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
  def apply(parserInfo: MetaParserInfo): ControlFlow = {
    new ControlFlow(parserInfo.systemImplicits.actorSystem, parserInfo.metaData, parserInfo.runInfo)
  }

  def controlConfiguration(socket: ActorRef): Configuration.Graph = {
    val name = "control-flow"
    Configuration.Graph(
      Seq(
        Configuration.Node(
          name,
          List("the-button"),
          List("the-led"),
          MetaData(
            Some(name),
            None,
            BlockType.Automaton,
            "default-control",
            "control-flow",
            Json.asciiStringify(Json.obj(
              "socket" -> socket.path.name
            ))
          )
        )
      ),
      List("the-button"),
      List("the-led"),
      Map.empty
    )
  }

  case class Params(socket: String)

  implicit val paramsFormat: OFormat[Params] = Json.format
}
