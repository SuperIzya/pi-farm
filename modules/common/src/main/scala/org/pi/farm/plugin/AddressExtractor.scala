package org.pi.farm.plugin
import org.pi.farm.model.Address

import zio.{Chunk, Task, ZIO}

sealed trait AddressExtractor[A] {
  def extractFrom(addresses: Chunk[Address]): Task[AddressFrom[A]]
}

object AddressExtractor {
  given scalar: [A]
    => (Address =:= AddressFrom[A])
    => (NotTuple[A]) => AddressExtractor[A] {
    def extractFrom(addresses: Chunk[Address]): Task[AddressFrom[A]] =
      ZIO.attempt(addresses.head)
  }

  given tuple2: [A, B]
    => (NotTuple[A])
    => (NotTuple[B])
    => (Address =:= AddressFrom[A])
    => (Address =:= AddressFrom[B])
    => ((Address, Address) =:= AddressFrom[(A, B)]) => AddressExtractor[(A, B)] {
    def extractFrom(addresses: Chunk[Address]): Task[AddressFrom[(A, B)]] =
      ZIO.attempt((addresses(0), addresses(1)))
  }

  given incTuple: [H, T <: Tuple]
    => (E: AddressExtractor[T])
    => (NotTuple[H])
    => (Address =:= AddressFrom[H])
    => (AddressFrom[T] =:= MapToAddress[T])
    => (Address *: MapToAddress[T] =:= AddressFrom[H *: T]) => AddressExtractor[H *: T] {
    def extractFrom(addresses: Chunk[Address]): Task[AddressFrom[H *: T]] =
      E.extractFrom(addresses.tail).map(addresses.head *: _)
  }
}
