package test.builder

import akka.stream.{ Inlet, Outlet }
import akka.stream.scaladsl.{ Sink, Source }
import com.ilyak.pifarm.State.Implicits._
import com.ilyak.pifarm.Types.GRun
import com.ilyak.pifarm.control.configuration.Builder
import com.ilyak.pifarm.flow.configuration.Configuration.Graph
import com.ilyak.pifarm.flow.configuration.Connection.{ External, Sockets }
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration }
import com.ilyak.pifarm.plugins.PluginLocator
import org.scalatest.{ FeatureSpec, GivenWhenThen, Matchers }
import test.builder.Data.{ Test1, TestData }

class BuilderTests extends FeatureSpec with GivenWhenThen with Matchers with GraphSpecs {

  implicit val loader: PluginLocator = new PluginLocator(Map("test" -> TestManifest))


  scenario("Builder should correctly process empty configuration") {
    Given("an empty configuration")
    val graph = emptyGraph

    When("it is built")
    val g = Builder.build(graph, Map.empty, Map.empty)

    Then("result should be Right but empty Shape")
    g should be('right)
    g.right.get should be(empty)
  }

  scenario("Builder should correctly process simple flow") {
    Given("a configuration with simple flow")
    val graph = Graph(
      Seq(Configuration.Node(
        "1",
        List("in"),
        List("out"),
        Configuration.MetaData(
          Some("test-flow"),
          None,
          BlockType.Automaton,
          "test",
          "flow",
          ""
        )
      )),
      List("in"),
      List("out"),
      Map.empty
    )

    When("it is built")
    val in = Source.fromIterator(() => new Iterator[TestData] {
      override def hasNext: Boolean = true

      override def next(): TestData = TestData(1)
    })

    val out = Sink.foreach[Test1.type](println(_))

    val inRun: GRun[Outlet[TestData]] = state => implicit b => {
      val (st, let) = state.getOrElse("in", ss => implicit bb => {
        val src = bb add in
        (ss, Sockets(Map.empty, Map("in" -> src.out)))
      })
      (st, let.outputs("in").as[TestData])
    }

    val outRun: GRun[Inlet[Test1.type]] = state => implicit b => {
      val (st, let) = state.getOrElse("out", ss => bb => {
        val dst = bb add out
        (ss, Sockets(Map("out" -> dst.in), Map.empty))
      })
      (st, let.inputs("out").as[Test1.type])
    }

    val g = Builder.build(graph,
      Map("in" -> External.In("in", "", inRun)),
      Map("out" -> External.Out("out", "", outRun))
    )

    Then("result should be Right")
    g should be('right)
  }
}
