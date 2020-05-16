package test.builder

import com.ilyak.pifarm.{ Command, Measurement, Units }

object Data {
  case class TestData(value: Float) extends Measurement[Float]
  object TestData {
    implicit val units = new Units[TestData] {
      override val name: String = "TestData"
    }
  }
  case object Test1 extends Command ("test1") {
    implicit val unit = new Units[Test1.type] {
      override val name: String = "Test1"
    }
  }

}
