package test.builder

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.BuildResult
import com.ilyak.pifarm.Types.BuildResult
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.Node
import test.builder.Data.{Test1, TestData}

object SimpleFlow extends FlowAutomaton[TestData, Test1.type] {
  override def flow(conf: Node): BuildResult[Flow[TestData, Data.Test1.type, _]] =
    BuildResult.Result(Flow[TestData].map(_ => Test1))
}
