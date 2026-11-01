package org.pi.farm.model

import zio.json.{
  DeriveJsonCodec,
  DeriveJsonDecoder,
  DeriveJsonEncoder,
  JsonCodec,
  JsonDecoder,
  JsonEncoder
}

import scala.reflect.{Typeable, TypeTest}

import language.experimental.captureChecking

/** A wrapper class that holds a value of type `T` along with an associated measurement's error of
  * the same type.
  *
  * @param value
  * @param sigma
  *   the associated error (standard deviation)
  */
final case class WithError[T](value: T, sigma: T)
object WithError {
  type Or[T] = Either[T, WithError[T]]

  given [T: JsonEncoder]: JsonEncoder[WithError[T]] = DeriveJsonEncoder.gen[WithError[T]]
  given [T: JsonDecoder]: JsonDecoder[WithError[T]] = DeriveJsonDecoder.gen[WithError[T]]

  object Or {
    given [T]
      => JsonEncoder[T]
      => JsonEncoder[WithError[T]]
      => JsonEncoder[Or[T]] =
      JsonEncoder[T].either(JsonEncoder[WithError[T]])

    given [T]
      => JsonDecoder[T]
      => JsonDecoder[WithError[T]]
      => JsonDecoder[Or[T]] =
      JsonDecoder[T].orElseEither(JsonDecoder[WithError[T]])

    given [T]
      => (encoder: JsonEncoder[Or[T]])
      => (decoder: JsonDecoder[Or[T]])
      => JsonCodec[Or[T]] =
      JsonCodec(encoder, decoder)

    extension [T](v: Or[T]) {
      def value: T = v.fold(identity, _.value)
    }
  }
}
