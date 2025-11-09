package org.pi.farm.ws.serialization

import org.pi.farm.generators.ModelGenerators.nameGen
import org.pi.farm.ws.Partial
import zio.test.Gen

object Generators {

  given partialGen: Gen[Any, Partial] = for {
    id   <- nameGen
    cnt  <- Gen.int(3, 10)
    data <- Gen.alphaNumericStringBounded(15536, 15536)
  } yield Partial(id, data, cnt, cnt * 10)
}
