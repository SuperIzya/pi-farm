package test.builder

import akka.stream.scaladsl.RunnableGraph
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
}
