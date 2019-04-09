package test.builder

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.BuildResult
import com.ilyak.pifarm.Types.{ BuildResult, GBuilder, SMap }
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.ConfigurableContainer
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.flow.configuration.Connection.{ In, Out, Sockets }
import com.ilyak.pifarm.flow.configuration.ShapeConnections.AutomatonConnections
import test.builder.Data.{ Test1, TestData }

object SimpleContainer extends ConfigurableContainer {
  override def buildShape(node: Configuration.Node, inner: AutomatonConnections): BuildResult[GBuilder[Sockets]] =
    BuildResult.Result { implicit b =>
      val inFlow = Flow[TestData]
      val outFlow = Flow[Test1.type]
      val input = b add inFlow
      val output = b add outFlow

      Sockets(
        Map(
          node.inputs.head -> input.in,
          "cont-out" -> output.in,
        ),
        Map(
          "cont-in" -> input.out,
          node.outputs.head -> output.out
        )
      )
    }

  override def inputFlows(node: Configuration.Node, inner: SMap[In[_]]): (Seq[In[_]], Seq[Out[_]]) =
    Seq(In[TestData](node.inputs.head, node.id)) ->
      Seq(Out(inner.head._1, _.outputs("cont-in").as[TestData], node.id))

  override def outputFlows(node: Configuration.Node, inner: SMap[Out[_]]): (Seq[In[_]], Seq[Out[_]]) =
    Seq(In(inner.head._1, _.inputs("cont-out").as[Test1.type], node.id)) ->
      Seq(Out[Test1.type](node.outputs.head, node.id))
}
