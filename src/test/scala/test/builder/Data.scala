package test.builder

import com.ilyak.pifarm.{Command, Measurement}

object Data {
  case class TestData(value: Float) extends Measurement
  case object Test1 extends Command ("test1")

}
