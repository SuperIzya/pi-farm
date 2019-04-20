package test.builder

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.Result
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.Configuration.Node
import test.builder.Data.{ Test1, TestData }

object ReverseFlow extends FlowAutomaton[Test1.type, TestData] {
  override def flow(conf: Node): Result[Flow[Data.Test1.type, TestData, _]] =
    Result.Res(Flow[Test1.type].map(_ => TestData(1)))
}
