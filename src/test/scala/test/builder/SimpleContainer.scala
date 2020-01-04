package test.builder

import akka.stream.scaladsl.Flow
import com.ilyak.pifarm.flow.configuration.ConfigurableNode._
import com.ilyak.pifarm.flow.configuration.Configuration.ParseMeta
import com.ilyak.pifarm.flow.configuration.Connection.{In, Out, Sockets}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.AutomatonConnections
import com.ilyak.pifarm.flow.configuration._
import com.ilyak.pifarm.types.{GBuilder, Result, SMap}
import test.builder.Data.{Test1, TestData}

object SimpleContainer extends ConfigurableContainer {
  implicit val cont = new NodeCompanion[SimpleContainer.type] {
    override val inputs: List[ConfigurableNode.XLet] =
      List(XLet[TestData]("in"))
    override val outputs: List[ConfigurableNode.XLet] =
      List(XLet[Test1.type]("out"))
    override val blockType: BlockType = BlockType.Container
    override val name: String = "container"
    override val creator: ParseMeta[SimpleContainer.type] = _ => SimpleContainer
  }

  override def buildShape(
    node: Configuration.Node,
    inner: AutomatonConnections
  ): Result[GBuilder[Sockets]] =
    Result.Res { implicit b =>
      val inFlow = Flow[TestData]
      val outFlow = Flow[Test1.type]
      val input = b add inFlow
      val output = b add outFlow

      Sockets(
        Map(node.inputs.head -> input.in, "cont-out" -> output.in),
        Map("cont-in" -> input.out, node.outputs.head -> output.out)
      )
    }

  override def inputFlows(
    node: Configuration.Node,
    inner: SMap[In[_]]
  ): Result[(Seq[In[_]], Seq[Out[_]])] =
    Result.Res(
      Seq(In[TestData](node.inputs.head, node.id)) ->
        Seq(Out[TestData](inner.head._1, node.id))
    )

  override def outputFlows(
    node: Configuration.Node,
    inner: SMap[Out[_]]
  ): Result[(Seq[In[_]], Seq[Out[_]])] =
    Result.Res(
      Seq(In[Test1.type](inner.head._1, node.id)) ->
        Seq(Out[Test1.type](node.outputs.head, node.id))
    )
}
