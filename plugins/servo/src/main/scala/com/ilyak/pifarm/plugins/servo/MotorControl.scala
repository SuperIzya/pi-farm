package com.ilyak.pifarm.plugins.servo

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types.{ GBuilder, Result }
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{ ConfigurableAutomaton, NodeCompanion, XLet }
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo, ParseMeta }
import com.ilyak.pifarm.flow.configuration.Connection.Sockets
import com.ilyak.pifarm.flow.configuration.{ BlockType, ConfigurableNode, Configuration, Connection }
import com.ilyak.pifarm.plugins.servo.MotorDriver.Spin
import com.ilyak.pifarm.{ Result, RunInfo }

class MotorControl(system: ActorSystem,
                   metaData: MetaData,
                   runInfo: RunInfo)
  extends ConfigurableAutomaton {
  override def inputs(node: Configuration.Node): Result[Seq[Connection.In[_]]] =
    Result.Res(Seq.empty)

  override def outputs(node: Configuration.Node): Result[Seq[Connection.Out[_]]] =
    Result.Res(Seq(Connection.Out[Spin]("the-spin", node.id)))

  override def buildShape(node: Configuration.Node): Result[GBuilder[Connection.Sockets]] = {
    val source = Source.actorRef[Spin](1, OverflowStrategy.dropHead)
      .mapMaterializedValue(a => {
        val actor = system.actorOf(MotorActor.props(runInfo))
        actor ! Subscribe(a)
        a
      })

    Result.Res { implicit b =>
      val src = b add source
      Sockets(Map.empty, Map("the-spin" -> src.out))
    }
  }
}

object MotorControl {
  implicit val comp = new NodeCompanion[MotorControl] {
    override val inputs: List[ConfigurableNode.XLet] = List.empty
    override val outputs: List[ConfigurableNode.XLet] =
      List(XLet[Spin]("the-spin"))
    override val blockType: BlockType = BlockType.Automaton
    override val name = "simple-motor-control"
    override val creator: ParseMeta[MotorControl] = MotorControl(_)
  }

  def apply(parserInfo: MetaParserInfo): MotorControl =
    new MotorControl(parserInfo.systemImplicits.actorSystem, parserInfo.metaData, parserInfo.runInfo)

  val configuration = Configuration.Graph(
    comp.name,
    Seq(
      Configuration.Node(
        comp.name,
        comp.inputNames,
        comp.outputNames,
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
    comp.outputNames,
    Map.empty
  )
}
