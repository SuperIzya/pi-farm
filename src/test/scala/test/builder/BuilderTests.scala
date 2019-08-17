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

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

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
  val tickTimeout = 100 milli
  val in: Source[TestData, _] = Source.tick(Duration.Zero, tickTimeout, TestData(1))
    .delay(tickTimeout).take(triesCount)

  val out: Sink[Test1.type, _] = Flow[Test1.type].map(_ => 1).fold(0)(_ + _).to(ActorSink[Int](self))

  val outputs = Map("in" -> External.Out[TestData]("in", "", in))
  val inputs = Map("out" -> External.In[Test1.type]("out", "", out))

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

    When("it is built")
    val g = Builder.build(emptyGraph, Map.empty, Map.empty)

    Then("the result should be Right")
    g should be('right)
    When("the result is run")
    g.right.get.run()
    Then("no messages should be received")
    expectNoMessage()
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

  scenario("Built graph's kill switch should work") { implicit locator =>
    val multi = 1
    Given(s"a correct configuration with $multi consumers/producers")
    val graph = multiGraph(multi)
    When("it is built")

    val flow = Flow[Test1.type]
      .statefulMapConcat({
        val counter: Iterable[Int] = new Iterable[Int] {
          var c = 0
          val i: Iterator[Int] = new Iterator[Int] {
            override def hasNext: Boolean = true

            override def next(): Int = { c += 1; c }
          }

          override def iterator: Iterator[Int] = i
        }
        () => _ => counter
      })
      .to(ActorSink[Int](self))
    val ins = Map("out" -> External.In[Test1.type]("in", "", flow))
    val g = Builder.build(graph, ins, outputs)

    Then("the result should be Right")
    g should be('right)

    When("it is started and")
    val ks1 = g.right.get.run()
    ks1.shutdown()
    When("stopped right after start")
    Then("no message should be received")
    expectNoMessage()


    When("it is started and")
    val ks2 = g.right.get.run()
    expectMsg(1)
    ks2.shutdown()
    When("stopped after first message")
    Then("no more messages should be received")
    expectNoMessage()
  }
}
