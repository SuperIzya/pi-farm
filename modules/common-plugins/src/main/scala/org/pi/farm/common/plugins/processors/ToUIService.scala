package org.pi.farm.common.plugins.processors

import org.pi.farm.runtime.{Controllers}
import zio.ZIO

trait ToUIService {
  val ToUIService =
    for {
      controllers <- ZIO.service[Controllers]
    } yield ()
}
