package test.builder

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.Node
import com.ilyak.pifarm.types.Result
import test.builder.Data.{Test1, TestData}

object SimpleFlow extends FlowAutomaton[TestData, Test1.type] {
  implicit val companion =
    FlowAutomaton.getCompanion[TestData, Test1.type, SimpleFlow.type](
      "flow",
      _ => SimpleFlow
    )
  override def flow(conf: Node): Result[Flow[TestData, Data.Test1.type, _]] =
    Result.Res(Flow[TestData].map(_ => Test1))
}
