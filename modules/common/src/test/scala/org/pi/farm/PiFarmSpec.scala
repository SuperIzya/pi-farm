package org.pi.farm

import zio.test.ZIOSpecDefault
import zio.test.TestAspect
import zio.*

abstract class PiFarmSpec extends ZIOSpecDefault {
  override def aspects = Chunk(TestAspect.timed, TestAspect.timeout(10.seconds), TestAspect.parallel)
}
