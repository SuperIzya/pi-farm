package org.pi.farm.ws

import zio.ZIO
import zio.json.*

import ToData.{toData => toDataScalar}

object Utils {

  private[ws] class ToOption[D <: Data, A, R, E](val task: ZIO[R, E, A]) extends AnyVal {
    inline def frame[T](using evO: A <:< Option[T], ev: ToData[T, D]): ZIO[R, E, Option[String]] =
      task.map {
        _.map(ev(_).toJson(using JsonEncoder[Data]))
      }

  }

  extension [R, E, A](task: ZIO[R, E, A]) {

    private[ws] def toData[D <: Data](using ev: ToData[A, D]): ZIO[R, E, Option[String]] =
      task.map(res => Some(res.toDataScalar.toJson))

    private[ws] def asData(using ev: A <:< Data): ZIO[R, E, Option[String]] =
      task.map(res => Some(ev(res).toJson))

    private[ws] inline def asOptional[B](using
      ev: A <:< Option[B],
      evb: B <:< Data
    ): ZIO[R, E, Option[String]] =
      task.map { ev(_).map(evb(_).toJson) }

    private[ws] def toOptional[D <: Data]: ToOption[D, A, R, E] = new ToOption[D, A, R, E](task)
  }
}
