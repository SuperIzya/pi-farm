package com.ilyak.pifarm.configuration.control

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.{ Result, RunInfo, SystemImplicits }
import com.ilyak.pifarm.Types.Result
import com.ilyak.pifarm.configuration.control.ControlFlow.Params
import com.ilyak.pifarm.driver.control.{ ButtonEvent, LedCommand }
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.flow.configuration.Configuration.MetaData
import play.api.libs.json.{ JsError, JsSuccess, Json, OFormat }

class ControlFlow(system: ActorSystem,
                  metaData: MetaData,
                  runInfo: RunInfo)
  extends FlowAutomaton[LedCommand, ButtonEvent] {

  override def flow(conf: Configuration.Node): Result[Flow[LedCommand, ButtonEvent, _]] =
    Json.fromJson[Params](Json.parse(metaData.params)) match {
      case JsError(errors) => Result.Err(s"$errors")
      case JsSuccess(value, _) =>

    }

}

object ControlFlow {
  def apply(meta: MetaData, system: SystemImplicits, runInfo: RunInfo): ControlFlow = {
    new ControlFlow(system.actorSystem, meta, runInfo)
  }

  case class Params(socketActor: String)
  implicit val paramsFormat: OFormat[Params] = Json.format


}
