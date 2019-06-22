package com.ilyak.pifarm.plugins.servo

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types.{ GBuilder, Result }
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.ConfigurableAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo }
import com.ilyak.pifarm.flow.configuration.Connection.Sockets
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration, Connection }
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
  val name = "simple-motor-control"

  def apply(parserInfo: MetaParserInfo): MotorControl =
    new MotorControl(parserInfo.systemImplicits.actorSystem, parserInfo.metaData, parserInfo.runInfo)

  val configuration = Configuration.Graph(
    name,
    Seq(
      Configuration.Node(
        name,
        List.empty,
        List("the-spin"),
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
    List.empty,
    List("the-spin"),
    Map.empty
  )
}
