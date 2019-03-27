package test.builder

import akka.stream.{SinkShape, SourceShape}
import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.Build.BuildResult
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.FlowAutomaton
import com.ilyak.pifarm.flow.configuration.{Configuration, Connection}
import test.builder.Data.{Test1, TestData}

object SimpleFlow extends FlowAutomaton[TestData, Test1.type] {
  override def flow(conf: Configuration.Node): BuildResult[Flow[TestData, Data.Test1.type, _]] =
    BuildResult.Result(Flow[TestData].map(_ => Test1))

  override def input(conf: Configuration.Node, in: SinkShape[TestData]): Connection.In[_] =
    Connection(conf.inputs.head, in)

  override def output(conf: Configuration.Node, out: SourceShape[Data.Test1.type]): Connection.Out[_] =
    Connection(conf.outputs.head, out)
}
