package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.{<:!<, =:!=, Inlet, Outlet, NotTuple}
import org.pi.farm.runtime.Environment
import org.pi.farm.model.Name
import zio.json.JsonCodec

import scala.compiletime.summonInline
import zio.{Ref, UIO, ZIO}
import scala.annotation.targetName
import zio.stream.ZStream

trait Flow {

  given [A, B]: Conversion[A => B, WithP[Unit, A, B]] with
    def apply(f: A => B): WithP[Unit, A, B] = (_: Unit) ?=> (a: A) => f(a)

  transparent inline def from[In](inline inlets: In) = ${ Builder.source[In]('inlets) }

  transparent inline def to[Out](inline outlets: Out) = ${ Builder.sink[Out]('outlets) }
}
