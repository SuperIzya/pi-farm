package test.builder

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.BuildResult
import com.ilyak.pifarm.Types.BuildResult
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.Node
import test.builder.Data.{ Test1, TestData }

object ReverseFlow extends FlowAutomaton[Test1.type, TestData] {
  override def flow(conf: Node): BuildResult[Flow[Data.Test1.type, TestData, _]] =
    BuildResult.Result(Flow[Test1.type].map(_ => TestData(1)))
}
