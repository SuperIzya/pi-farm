package com.ilyak.pifarm.plugins

import com.ilyak.pifarm.Types.TDriverCompanion
import com.ilyak.pifarm.driver.DriverManifest
import com.ilyak.pifarm.{ ManifestLocator, SystemImplicits }

case class DriverLocator(drivers: List[TDriverCompanion])

object DriverLocator extends ManifestLocator {
  def apply(impl: SystemImplicits): DriverLocator =
    new DriverLocator(
      locate[DriverManifest](impl.actorSystem.log)
        .map(_.drivers)
        .foldLeft(List.empty[TDriverCompanion])(_ ++ _)
    )
}
