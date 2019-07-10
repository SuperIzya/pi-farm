package com.ilyak.pifarm.plugins.temperature

import akka.event.Logging
import akka.stream.{ Attributes, ThrottleMode }
import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.Result
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.{ FlowAutomaton, NodeCompanion, XLet }
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo, ParseMeta }
import com.ilyak.pifarm.flow.configuration.{ BlockType, ConfigurableNode, Configuration }
import com.ilyak.pifarm.plugins.servo.MotorDriver.{ Spin, SpinLeft, SpinRight, SpinStop }
import com.ilyak.pifarm.plugins.temperature.TempDriver.Humidity

import scala.concurrent.duration._
import scala.language.postfixOps

class HumidityFlow extends FlowAutomaton[Humidity, Spin] {
  override def flow(conf: Configuration.Node): Result[Flow[Humidity, Spin, _]] =
    Result.Res {
      Flow[Humidity]
        .throttle(1, 1 second, 1, ThrottleMode.Shaping)
        .log(HumidityFlow.name)
        .withAttributes(Attributes.logLevels(
          onFailure = Logging.ErrorLevel,
          onFinish = Logging.WarningLevel,
          onElement = Logging.DebugLevel
        ))
        .statefulMapConcat(() => {
          var last: Float = -1
          x => {
            if (x.value > last) {
              last = x.value
              List(Spin(SpinLeft))
            } else if (x.value < last) {
              last = x.value
              List(Spin(SpinRight))
            } else List(Spin(SpinStop))
          }
        })
        .log(HumidityFlow.name)
        .withAttributes(Attributes.logLevels(
          onFailure = Logging.ErrorLevel,
          onFinish = Logging.WarningLevel,
          onElement = Logging.DebugLevel
        ))
    }
}

object HumidityFlow {

  implicit val comp = new NodeCompanion[HumidityFlow] {
    override val inputs: List[ConfigurableNode.XLet] =
      List(XLet[Humidity]("humidity"))
    override val outputs: List[ConfigurableNode.XLet] =
      List(XLet[Spin]("the-spin"))
    override val blockType: BlockType = BlockType.Automaton
    override val name = "Humidity motor control"
    override val creator: ParseMeta[HumidityFlow] = HumidityFlow(_)
  }

  val name = comp.name

  def apply(info: MetaParserInfo): HumidityFlow = new HumidityFlow()


  val configuration: Configuration.Graph = Configuration.Graph(
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
