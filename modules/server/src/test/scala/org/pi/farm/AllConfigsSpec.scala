package org.pi.farm

import org.pi.farm.service.FileService
import org.pi.farm.storage.AppConfiguration

import zio.*
import zio.test.*

object AllConfigsSpec extends PiFarmSpec {
  def spec = suite("AllConfigsTest")(
    test("Should load all configurations") {
      (Main.preBootstrap >+> Main.configLayer).build.as(assertCompletes)
    },
    test("shares file-service glob settings through AppConfig") {
      for {
        configs  <- (Main.preBootstrap >+> Main.configLayer).build
        files     = configs.get[FileService.Config]
        app      <- (ZLayer.succeed(files) ++ fake.ProcessingUnitsRepositoryFake.empty >>>
                      AppConfiguration.live).build
        settings <- app.get[AppConfiguration].get
      } yield assertTrue(
        files.codeFileGlobs.contains("platformio.ini"),
        settings.codeFileGlobs == files.codeFileGlobs
      )
    }
  )
}
