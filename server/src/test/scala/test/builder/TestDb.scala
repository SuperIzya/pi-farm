package test.builder

import com.typesafe.slick.testkit.util.H2TestDB
class TestDb(name: String) extends H2TestDB("h2mem", false) {
  val url = s"jdbc:h2:mem:$name"
  override def isPersistent = false
}
