package test.builder

import akka.actor.{ ActorRef, ActorSystem }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, RunnableGraph, Sink, Source }
import com.ilyak.pifarm.BroadcastActor.Subscribe
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.flow.ActorSink
import com.ilyak.pifarm.flow.configuration.Configuration.{ Graph, Node }
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration }
import org.scalatest.Matchers
import org.scalatest.enablers.Emptiness
import test.builder.Data.{ Test1, TestData }

import scala.concurrent.duration.{ Duration, _ }
import scala.language.postfixOps

trait GraphSpecs { this: Matchers =>

  val emptyGraph = Graph("", Seq.empty, List.empty, List.empty, Map.empty)
  val triesCount = 5
  val tickTimeout = 300 milli

  def outputs(): SMap[External.Out[_]] = {
    val in: Source[TestData, _] = Source.tick(Duration.Zero, tickTimeout, TestData(1))
      .take(triesCount)
    Map("in" -> External.Out[TestData]("in", "", in))
  }

  def inputs(s: ActorRef): SMap[External.In[_]] = {
    val out: Sink[Test1.type, _] = Flow[Test1.type].map(_ => 1).fold(0)(_ + _).to(ActorSink[Int](s))
    Map("out" -> External.In[Test1.type]("out", "", out))
  }

  implicit val graphEmptyMatcher: Emptiness[RunnableGraph[_]] = (thing: RunnableGraph[_]) => {
    val builder = thing.traversalBuilder
    builder.attributes.attributeList.isEmpty &&
      builder.inSlots == 0 &&
      builder.unwiredOuts == 0
  }


  def counterIns(s: ActorRef): SMap[External.In[_]] = {
    val flow = Flow[Test1.type]
      .scan(0)((c, _) => c + 1)
      .filter(_ > 0)
      .map(x => x)
      .to(ActorSink[Int](s))
    Map(
      "out" -> External.In[Test1.type]("in", "", flow),
      "out2" -> External.In[Test1.type]("in2", "", flow),
    )
  }

  def actorSource(test: ActorRef)(implicit system: ActorSystem): (ActorRef, SMap[External.Out[_]]) = {
    val actor = system.actorOf(TestSourceActor.props(test))

    val src: Source[TestData, _] = Source.actorRef[Any](1, OverflowStrategy.dropHead)
      .mapMaterializedValue(a => actor ! Subscribe(a))
      .map(_ => {
        TestData(1)
      })
    actor -> Map(
      "in" -> External.Out("in", "", src),
      "in2" -> External.Out("in", "", src),
    )
  }

  def simpleGraph: Graph = Graph(
    "simple-flow",
    Seq(simpleFlow()),
    List("in"),
    List("out"),
    Map.empty
  )

  def reverseGraph: Graph = Graph(
    "reverse-flow",
    Seq(reverseFlow()),
    List("in"),
    List("out"),
    Map.empty
  )

  def reverseFlow(n: Int = 1, in: String = "in", out: String = "out"): Configuration.Node = Configuration.Node(
    n.toString,
    List(in),
    List(out),
    Configuration.MetaData(
      Some(s"test-flow-$n"),
      None,
      BlockType.Automaton,
      "test",
      "reverse-flow",
      ""
    )
  )

  def simpleFlow(n: Int = 1, in: String = "in", out: String = "out"): Configuration.Node = Configuration.Node(
    n.toString,
    List(in),
    List(out),
    Configuration.MetaData(
      Some(s"test-flow-$n"),
      None,
      BlockType.Automaton,
      "test",
      "flow",
      ""
    )
  )

  def multiGraph(n: Int = 5, in: String = "in", out: String = "out"): Graph = Graph(
    s"multi-$n-parts",
    (1 to n).map(simpleFlow(_, in, out)),
    List(in),
    List(out),
    Map.empty
  )

  def containerNode(id: String = "cont"): Node = Configuration.Node(
    id,
    List("in"),
    List("out"),
    Configuration.MetaData(
      Some(s"test-container-$id"),
      None,
      BlockType.Container,
      "test",
      "container",
      ""
    )
  )

  def container(inner: Int = 1): Graph = {

    val id = "cont"
    val cont = containerNode(id)

    Graph(
      "container",
      Seq(cont),
      List("in"),
      List("out"),
      Map(id -> multiGraph(inner, s"$id-in", s"$id-out"))
    )
  }

  def unconnectedInner(inner: Int = 1): Graph = {
    val id = "cont"
    val cont = containerNode(id)
    Graph(
      "outer",
      Seq(cont),
      List("in"),
      List("out"),
      Map(id -> Graph(
        s"inner-$id",
        Seq(simpleFlow(0, "in-0", "out-0")) ++
          (1 to inner).map(simpleFlow(_, s"$id-in", s"$id-out")),
        List(s"$id-in"),
        List(s"$id-out"),
        Map.empty
      ))
    )
  }
}
