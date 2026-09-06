package org.pi.farm.ws.serialization

import org.pi.farm.ws.Partial

import zio.test.Gen

object Generators {

  given partialGen: Gen[Any, Partial] = for {
    id   <- Gen.uuid.map(_.toString)
    cnt  <- Gen.int(3, 10)
    data <- Gen.alphaNumericStringBounded(15536, 15536)
  } yield Partial(id, data, cnt, cnt * 10)
}
