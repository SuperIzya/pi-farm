package com.ilyak.pifarm.plugins.temperature

import akka.event.Logging
import akka.stream.{ Attributes, ThrottleMode }
import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.Result
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.{ MetaData, MetaParserInfo }
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration }
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
  def apply(info: MetaParserInfo): HumidityFlow = new HumidityFlow()

  val name = "Humidity motor control"
  val configuration: Configuration.Graph = Configuration.Graph(
    name,
    Seq(
      Configuration.Node(
        name,
        List("humidity"),
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
    List("humidity"),
    List("the-spin"),
    Map.empty
  )
}
