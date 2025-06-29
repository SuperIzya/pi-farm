package org.pi.farm

import zio.test.{ZIOSpecDefault, assertCompletes, suite, test}

object AllConfigsSpec extends ZIOSpecDefault {
  def spec = suite("AllConfigsTest")(
    test("Should load all configurations") {
      (Main.preBootstrap >+> Main.configLayer).build.as(assertCompletes)
    }
  )
}
