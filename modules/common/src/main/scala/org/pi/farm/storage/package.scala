package org.pi.farm

import doobie.*
import doobie.implicits.*
import org.pi.farm.common.*

package object storage {
  given peripheryDirectionMeta: Meta[PeripheryType.Direction] =
    Meta[String].imap(str => PeripheryType.Direction.valueOf(str))(dir => dir.toString)
}
