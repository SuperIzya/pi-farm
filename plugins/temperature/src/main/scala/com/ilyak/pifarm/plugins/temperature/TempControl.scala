package com.ilyak.pifarm.plugins.temperature

import akka.actor.{ ActorSystem, PoisonPill }
import akka.stream.scaladsl.Sink
import com.ilyak.pifarm.Types.{ GBuilder, Result }
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{ ConfigurableAutomaton, NodeCompanion, XLet }
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo, ParseMeta }
import com.ilyak.pifarm.flow.configuration.Connection.Sockets
import com.ilyak.pifarm.flow.configuration.{ BlockType, ConfigurableNode, Configuration, Connection }
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

  implicit val comp = new NodeCompanion[TempControl] {
    override val inputs: List[ConfigurableNode.XLet] =
      List(
        XLet[Temperature]("temperature"),
        XLet[Humidity]("humidity")
      )
    override val outputs: List[ConfigurableNode.XLet] = List.empty
    override val name = "Temperature & humidity sensor"
    override val blockType: BlockType = BlockType.Automaton
    override val creator: ParseMeta[TempControl] = TempControl(_)
  }

  def apply(parserInfo: MetaParserInfo): TempControl =
    new TempControl(parserInfo.systemImplicits.actorSystem, parserInfo.metaData, parserInfo.runInfo)



  val configuration: Configuration.Graph = Configuration.Graph(
    comp.name,
    Seq(
      Configuration.Node(
        comp.name,
        comp.inputNames,
        List.empty,
        meta = MetaData(
          Some(comp.name),
          None,
          comp.blockType,
          plugin = Manifest.pluginName,
          blockName = comp.name,
          params = ""
        )
      )
    ),
    comp.inputNames,
    List.empty,
    Map.empty
  )
}
