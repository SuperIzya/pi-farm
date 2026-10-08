package org.pi.farm

import zio.*
import zio.test.{TestAspect, ZIOSpecDefault}

abstract class PiFarmSpec extends ZIOSpecDefault {

  protected def defaultAspects =
    Chunk(TestAspect.timed, TestAspect.timeout(10.seconds), TestAspect.parallel)

  override def aspects = defaultAspects

}
