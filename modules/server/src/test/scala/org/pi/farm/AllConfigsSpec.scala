package org.pi.farm

import zio.test.{assertCompletes, suite, test, ZIOSpecDefault}

object AllConfigsSpec extends PiFarmSpec {
  def spec = suite("AllConfigsTest")(
    test("Should load all configurations") {
      (Main.preBootstrap >+> Main.configLayer).build.as(assertCompletes)
    }
  )
}
