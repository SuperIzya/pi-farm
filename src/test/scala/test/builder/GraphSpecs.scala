package test.builder

import akka.stream.scaladsl.RunnableGraph
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration }
import com.ilyak.pifarm.flow.configuration.Configuration.Graph
import org.scalatest.Matchers
import org.scalatest.enablers.Emptiness

trait GraphSpecs {
  this: Matchers =>

  val emptyGraph = Graph(Seq.empty, List.empty, List.empty, Map.empty)

  implicit val graphEmptyMatcher: Emptiness[RunnableGraph[_]] = (thing: RunnableGraph[_]) => {
    val builder = thing.traversalBuilder
    builder.attributes.attributeList.isEmpty &&
      builder.inSlots == 0 &&
      builder.unwiredOuts == 0
  }

  def simpleGraph: Graph = Graph(
    Seq(simpleFlow()),
    List("in"),
    List("out"),
    Map.empty
  )

  def simpleFlow(n: Int = 1): Configuration.Node = Configuration.Node(
    n.toString,
    List("in"),
    List("out"),
    Configuration.MetaData(
      Some(s"test-flow-$n"),
      None,
      BlockType.Automaton,
      "test",
      "flow",
      ""
    )
  )

  def multiGraph(n: Int = 5): Graph = Graph(
    (1 to n).map(simpleFlow),
    List("in"),
    List("out"),
    Map.empty
  )
}
