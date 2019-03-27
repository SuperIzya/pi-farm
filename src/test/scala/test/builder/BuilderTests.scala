package test.builder

import akka.stream.scaladsl.{Sink, Source}
import com.ilyak.pifarm.control.configuration.Builder
import com.ilyak.pifarm.flow.configuration.Configuration.Graph
import com.ilyak.pifarm.flow.configuration.{BlockType, Configuration, Connection}
import com.ilyak.pifarm.plugins.PluginLocator
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import test.builder.Data.{Test1, TestData}

class BuilderTests extends FeatureSpec with GivenWhenThen with Matchers with GraphSpecs {

  implicit val loader: PluginLocator = new PluginLocator(Map("test" -> TestManifest))


  scenario("Builder should correctly process empty configuration") {
    Given("an empty configuration")
    val graph = emptyGraph

    When("it is built")
    val g = Builder.build(graph, Map.empty, Map.empty)

    Then("result should be Right but empty Shape")
    g should be ('right)
    g.right.get should be (empty)

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
    val g = Builder.build(graph,
      Map("in" -> Connection.External[TestData]("in", Source(List.empty))),
      Map("out" -> Connection.External[Test1.type]("out", Sink.ignore))
    )

    Then("result should be Right")
    g should be ('right)
  }

}
