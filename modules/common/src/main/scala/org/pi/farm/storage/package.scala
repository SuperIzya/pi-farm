package org.pi.farm

import cats.Show
import doobie.*
import doobie.implicits.*
import org.h2.api.H2Type
import org.pi.farm.model.*
import org.pi.farm.model.given
import zio.json.*
import zio.json.ast.Json
import scala.language.implicitConversions

package object storage {
  given peripheryDirectionMeta: Meta[PeripheryType.Direction] =
    Meta[String].tiemap(PeripheryType.Direction.fromString)(PeripheryType.Direction.toString)

  private given Show[Array[Byte]] = Show.show(new String(_, "UTF-8"))

  given Meta[Json] = Meta[Array[Byte]]
    .tiemap(new String(_, "UTF-8").fromJson[Json])(_.toJson.getBytes("UTF-8"))

  extension (fr: Iterable[Fragment]) {
    def combine: Fragment = fr.reduce(_ ++ sql", " ++ _)
  }

  given Meta[ControllerId]       = Meta.IntMeta.imap[ControllerId](x => x)(x => x)
  given Meta[ControllerTypeId]   = Meta.IntMeta.imap[ControllerTypeId](x => x)(x => x)
  given Meta[PeripheryId]        = Meta.StringMeta.imap[PeripheryId](x => x)(x => x)
  given Meta[PeripheryTypeId]    = Meta.IntMeta.imap[PeripheryTypeId](x => x)(x => x)
  given Meta[ConfigurationId]    = Meta.IntMeta.imap[ConfigurationId](x => x)(x => x)
  given Meta[ControllerTypeName] = Meta.StringMeta.imap[ControllerTypeName](x => x)(x => x)
  given Meta[Name]               = Meta.StringMeta.imap[Name](x => x)(x => x)

}
