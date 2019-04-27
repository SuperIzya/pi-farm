package test.builder

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.Result
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.Node
import test.builder.Data.{Test1, TestData}

object SimpleFlow extends FlowAutomaton[TestData, Test1.type] {
  override def flow(conf: Node): Result[Flow[TestData, Data.Test1.type, _]] =
    Result.Res(Flow[TestData].map(_ => Test1))
}
