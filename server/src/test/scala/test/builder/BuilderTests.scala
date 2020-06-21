package test.builder

import java.util.UUID.randomUUID

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.plugins.PluginLocator
import com.ilyak.pifarm.{Default, RunInfo, SystemImplicits}
import com.typesafe.config.ConfigFactory
import org.scalatest.featurespec.FixtureAnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, Outcome}
import slick.jdbc.JdbcBackend.Database
import slick.util.AsyncExecutor

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class BuilderTests extends TestKit(
  ActorSystem("test-system")
)
  with FixtureAnyFeatureSpecLike
  with GivenWhenThen
  with Matchers
  with GraphSpecs
  with ImplicitSender
  with BeforeAndAfterAll {

  val asyncExecutor = new AsyncExecutor {
    override def executionContext: ExecutionContext = system.dispatcher

    override def close(): Unit = system.terminate()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  protected type FixtureParam = PluginLocator

  def withFixture(test: OneArgTest): Outcome = {
    val db = new TestDb(randomUUID.toString)
    val profile = db.profile
    val bdb: Database = db.createDB().asInstanceOf[Database]
    implicit val ec: ExecutionContext = system.dispatcher
    val sys = Default.System(ConfigFactory.empty())
    val p = PluginLocator(
      SystemImplicits(sys.actorSystem, sys.materializer, sys.config, bdb, profile),
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
    Builder.build(unconnectedInner(4), inputs(self), outputs()) should be('left)

    Given("a graph with incompatible connection")
    Then("the result should be left")
    Builder.build(reverseGraph, inputs(self), outputs()) should be('left)
  }

  scenario("Builder should correctly process simple flow") { implicit locator =>
    Given("a configuration with simple flow")
    val graph = simpleGraph

    When("it is built")

    val g = Builder.build(graph, inputs(self), outputs())

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

    val g = Builder.build(graph, inputs(self), outputs())

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

    val g = Builder.build(graph, inputs(self), outputs())

    Then("the result should be Right")
    g should be('right)

    Then(s"$triesCount messages should lead to ${ triesCount * inner } answers")
    g.right.get.run()

    expectMsg(triesCount * inner)
  }

  scenario("Built graph's kill switch right after start should work") { implicit locator =>


    val multi = 1
    Given(s"a correct configuration with $multi consumers/producers")
    val graph = multiGraph(multi)
    When("it is built")

    val g = Builder.build(graph, counterIns(self), outputs())

    Then("the result should be Right")
    g should be('right)

    When("it is started and")
    val ks = g.right.get.run()
    ks.shutdown()
    When("stopped right after start")
    Then("no message should be received")
    expectNoMessage()
  }

  scenario("Built graph's kill switch after first message should work") { implicit locator =>
    val multi = 1

    Given(s"a correct configuration with $multi consumers/producers")
    val graph = simpleGraph
    val (actor, src) = actorSource(self)
    watch(actor)
    When("it is built")
    val g = Builder.build(graph, counterIns(self), src)

    Then("the result should be Right")
    g should be('right)

    When("it is started and")
    val ks = g.right.get.run()
    actor ! Unit
    expectMsg('ack)
    expectMsg(1)

    When("stopped after first message")
    ks.shutdown()
    Then("no more messages should be received")
    expectTerminated(actor)
  }
}

