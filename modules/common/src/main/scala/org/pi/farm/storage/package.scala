package org.pi.farm

import cats.Show
import doobie.*
import doobie.implicits.*
import org.h2.api.H2Type
import org.pi.farm.model.*
import zio.json.*
import zio.json.ast.Json

package object storage {
  given peripheryDirectionMeta: Meta[PeripheryType.Direction] =
    Meta[String].tiemap(PeripheryType.Direction.fromString)(PeripheryType.Direction.toString)

  private given Show[Array[Byte]] = Show.show(new String(_, "UTF-8"))

  given Meta[Json] = Meta[Array[Byte]]
    .tiemap(new String(_, "UTF-8").fromJson[Json])(_.toJson.getBytes("UTF-8"))

  extension (fr: Iterable[Fragment]) {
    def combine: Fragment = fr.reduce(_ ++ sql", " ++ _)
  }
}
