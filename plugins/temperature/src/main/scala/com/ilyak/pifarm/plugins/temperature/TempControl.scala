package com.ilyak.pifarm.plugins.temperature

import akka.actor.{ ActorSystem, PoisonPill }
import akka.stream.scaladsl.Sink
import com.ilyak.pifarm.Types.{ GBuilder, Result }
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.ConfigurableAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo }
import com.ilyak.pifarm.flow.configuration.Connection.Sockets
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration, Connection }
import com.ilyak.pifarm.plugins.temperature.TempDriver.{ Humidity, Temperature }
import com.ilyak.pifarm.{ Result, RunInfo }

class TempControl(system: ActorSystem,
                  metaData: MetaData,
                  runInfo: RunInfo)
  extends ConfigurableAutomaton {
  override def inputs(node: Configuration.Node): Result[Seq[Connection.In[_]]] = Result.Res(Seq(
    Connection.In[Temperature]("temperature", node.id),
    Connection.In[Humidity]("humidity", node.id)
  ))

  override def outputs(node: Configuration.Node): Result[Seq[Connection.Out[_]]] =
    Result.Res(Seq.empty)

  override def buildShape(node: Configuration.Node): Result[GBuilder[Connection.Sockets]] = {
    val actor = system.actorOf(TempActor.props(runInfo))
    val sink = Sink.actorRef(actor, PoisonPill)
    Result.Res { implicit b =>
      val s1 = b add sink
      val s2 = b add sink
      Sockets(Map("temperature" -> s1.in, "humidity" -> s2.in), Map.empty)
    }
  }
}

object TempControl {
  def apply(parserInfo: MetaParserInfo): TempControl =
    new TempControl(parserInfo.systemImplicits.actorSystem, parserInfo.metaData, parserInfo.runInfo)

  val name = "Temperature & humidity sensor"

  val configuration: Configuration.Graph = Configuration.Graph(
    name,
    Seq(
      Configuration.Node(
        name,
        List("temperature", "humidity"),
        List.empty,
        meta = MetaData(
          Some(name),
          None,
          BlockType.Automaton,
          plugin = Manifest.pluginName,
          blockName = name,
          params = ""
        )
      )
    ),
    List("temperature", "humidity"),
    List.empty,
    Map.empty
  )
}
