package test.builder

import java.util.UUID.randomUUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.testkit.{ ImplicitSender, TestKit }
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.flow.ActorSink
import com.ilyak.pifarm.flow.configuration.Connection.External
import com.ilyak.pifarm.plugins.PluginLocator
import com.ilyak.pifarm.{ RunInfo, SystemImplicits }
import com.typesafe.config.ConfigFactory
import org.scalatest._
import slick.jdbc.JdbcBackend.Database
import slick.util.AsyncExecutor
import test.builder.Data.{ Test1, TestData }

import scala.concurrent.ExecutionContext

class BuilderTests extends TestKit(ActorSystem("test-system"))
  with fixture.FeatureSpecLike
  with GivenWhenThen
  with Matchers
  with GraphSpecs
  with ImplicitSender
  with BeforeAndAfterAll {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val asyncExecutor = new AsyncExecutor {
    override def executionContext: ExecutionContext = system.dispatcher

    override def close(): Unit = system.terminate()
  }

  val triesCount = 5
  val in: Source[TestData, _] = Source.fromIterator(() => new Iterator[TestData] {

    override def hasNext: Boolean = true

    override def next(): TestData = TestData(1)
  }).take(triesCount)

  val out: Sink[Test1.type, _] = Flow[Test1.type].map(_ => 1).fold(0)(_ + _).to(ActorSink[Int](self))

  val inputs = Map("in" -> External.In("in", "", in))
  val outputs = Map("out" -> External.Out("out", "", out))

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  protected type FixtureParam = PluginLocator

  def withFixture(test: OneArgTest): Outcome = {
    val db = new TestDb(randomUUID.toString)
    val profile = db.profile
    val bdb: Database = db.createDB().asInstanceOf[Database]
    val p = PluginLocator(
      SystemImplicits(system, materializer, ConfigFactory.empty(), bdb, profile),
      RunInfo.empty,
      Map("test" -> TestManifest)
    )
    withFixture(test.toNoArgTest(p))
  }

  scenario("Builder should correctly process empty configuration") { implicit locator =>
    Given("an empty configuration")
    val graph = emptyGraph

    When("it is built")
    val g = Builder.build(graph, Map.empty, Map.empty)

    Then("the result should be Right")
    g should be('right)
    Then("the result should be empty Shape")
    g.right.get should be(empty)
  }


  scenario("Builder should correctly process simple flow") { implicit locator =>
    Given("a configuration with simple flow")
    val graph = simpleGraph

    When("it is built")

    val g = Builder.build(graph, inputs, outputs)

    Then("the result should be Right")
    g should be('right)

    Then(s"$triesCount messages should lead to $triesCount answers")
    g.right.get.run()
    expectMsg(triesCount)
  }

  scenario("Builder should merge and broadcast") { implicit locator =>
    val multi = 4
    Given(s"a configuration with $multi consumers/producers")
    val graph = multiGraph(multi)
    When("it is built")

    val g = Builder.build(graph, inputs, outputs)

    Then("the result should be Right")
    g should be('right)

    Then(s"$triesCount messages should lead to ${ triesCount * multi }")
    g.right.get.run()

    expectMsg(triesCount * multi)
  }

  scenario("Builder should process container") { implicit locator =>
    val inner = 4
    Given(s"a graph with inner graph (of $inner flows)")
    val graph = container(inner)

    val g = Builder.build(graph, inputs, outputs)

    Then("the result should be Right")
    g should be('right)

    Then(s"$triesCount messages should lead to ${ triesCount * inner } answers")
    g.right.get.run()

    expectMsg(triesCount * inner)
  }

  scenario("Builder should return error") { implicit locator =>
    Given("a graph with no external connections")
    Then("the result should be Left")
    Builder.build(simpleGraph, Map.empty, Map.empty) should be('left)

    Given("a graph with unconnected internal connections")
    Then("the result should be Left")
    Builder.build(unconnectedInner(4), inputs, outputs) should be('left)

    Given("a graph with incompatible connection")
    Then("the result should be left")
    Builder.build(reverseGraph, inputs, outputs) should be('left)
  }
}
