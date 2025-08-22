package org.pi.farm.model

import zio.json.ast.Json

case class Configuration(
                          id: Int,
                          inbound: Set[Inbound],
                          outbound: Set[Outbound],
                          processingUnit: String,
                          additional: Option[Json] = None
)
