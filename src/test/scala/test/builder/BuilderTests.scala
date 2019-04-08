package test.builder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.testkit.{ ImplicitSender, TestKit }
import com.ilyak.pifarm.control.configuration.Builder
import com.ilyak.pifarm.flow.ActorSink
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.plugins.PluginLocator
import org.scalatest.{ BeforeAndAfterAll, FeatureSpecLike, GivenWhenThen, Matchers }
import test.builder.Data.{ Test1, TestData }

class BuilderTests extends TestKit(ActorSystem("test-system"))
  with FeatureSpecLike
  with GivenWhenThen
  with Matchers
  with GraphSpecs
  with ImplicitSender
  with BeforeAndAfterAll {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val loader: PluginLocator = new PluginLocator(Map("test" -> TestManifest))

  val triesCount = 5
  val in: Source[TestData, _] = Source.fromIterator(() => new Iterator[TestData] {

    override def hasNext: Boolean = true

    override def next(): TestData = TestData(1)
  }).take(triesCount)
  val out: Sink[Test1.type, _] = Flow[Test1.type].map(_ => 1).fold(0)(_ + _).to(ActorSink[Int](self))

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }
  scenario("Builder should correctly process empty configuration") {
    Given("an empty configuration")
    val graph = emptyGraph

    When("it is built")
    val g = Builder.build(graph, Map.empty, Map.empty)

    Then("result should be Right")
    g should be('right)
    Then("result should be empty Shape")
    g.right.get should be(empty)
  }

  scenario("Builder should correctly process simple flow") {
    Given("a configuration with simple flow")
    val graph = simpleGraph

    When("it is built")

    val g = Builder.build(graph,
      Map("in" -> External.In("in", "", in)),
      Map("out" -> External.Out("out", "", out))
    )

    Then("result should be Right")
    g should be('right)

    Then(s"$triesCount messages should lead to $triesCount answers")
    g.right.get.run()
    expectMsg(triesCount)
  }

  scenario("Builder should merge multiple producers and broadcast to multiple consumers") {
    val multi = 4
    Given(s"a configuration with $multi consumers/producers")
    val graph = multiGraph(multi)
    When("it is built")

    val g = Builder.build(graph,
      Map("in" -> External.In("in", "", in)),
      Map("out" -> External.Out("out", "", out))
    )

    Then("result should be Right")
    g should be('right)

    Then(s"$triesCount messages should lead to ${triesCount * multi}")
    g.right.get.run()

    expectMsg(triesCount * multi)
  }
}
