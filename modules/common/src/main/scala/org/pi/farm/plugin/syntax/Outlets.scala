package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.{Inlet, NotTuple, Outlet}
import zio.ZIO
import zio.json.JsonCodec

trait Outlets[In <: NonEmptyTuple, Out, ParamsType, R, E](using JsonCodec[ParamsType]) { self =>
  def inlets: Tuple.Map[In, Inlet]

  def processor: In => ParamsType ?=> ZIO[R, E, Out]

  def to(outlet: Outlet[Out])(using NotTuple[Out]): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = Tuple1[self.Out]

      val inlets: Tuple.Map[In, Inlet]      = self.inlets
      val outlets: Tuple1[Outlet[self.Out]] = Tuple1(outlet)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor andThen (_.map(Tuple1(_)))
    }

  def to[O](outlet: O)(using Out <:< Tuple, O =:= Tuple.Map[Out, Outlet]): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: O                   = outlet

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2](o1: Outlet[O1], o2: Outlet[O2])(using Out =:= (O1, O2)): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet]      = self.inlets
      val outlets: (Outlet[O1], Outlet[O2]) = (o1, o2)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3](o1: Outlet[O1], o2: Outlet[O2], o3: Outlet[O3])(using
    Out =:= (O1, O2, O3)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet]                  = self.inlets
      val outlets: (Outlet[O1], Outlet[O2], Outlet[O3]) = (o1, o2, o3)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4](o1: Outlet[O1], o2: Outlet[O2], o3: Outlet[O3], o4: Outlet[O4])(using
    Out =:= (O1, O2, O3, O4)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet]                              = self.inlets
      val outlets: (Outlet[O1], Outlet[O2], Outlet[O3], Outlet[O4]) = (o1, o2, o3, o4)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5](o1: Outlet[O1], o2: Outlet[O2], o3: Outlet[O3], o4: Outlet[O4], o5: Outlet[O5])(using
    Out =:= (O1, O2, O3, O4, O5)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet]                                          = self.inlets
      val outlets: (Outlet[O1], Outlet[O2], Outlet[O3], Outlet[O4], Outlet[O5]) = (o1, o2, o3, o4, o5)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet]                                                      = self.inlets
      val outlets: (Outlet[O1], Outlet[O2], Outlet[O3], Outlet[O4], Outlet[O5], Outlet[O6]) = (o1, o2, o3, o4, o5, o6)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet]                                                                  = self.inlets
      val outlets: (Outlet[O1], Outlet[O2], Outlet[O3], Outlet[O4], Outlet[O5], Outlet[O6], Outlet[O7]) =
        (o1, o2, o3, o4, o5, o6, o7)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (Outlet[O1], Outlet[O2], Outlet[O3], Outlet[O4], Outlet[O5], Outlet[O6], Outlet[O7], Outlet[O8]) =
        (o1, o2, o3, o4, o5, o6, o7, o8)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets
        : (Outlet[O1], Outlet[O2], Outlet[O3], Outlet[O4], Outlet[O5], Outlet[O6], Outlet[O7], Outlet[O8], Outlet[O9]) =
        (o1, o2, o3, o4, o5, o6, o7, o8, o9)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14],
    o15: Outlet[O15]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14],
        Outlet[O15]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14],
    o15: Outlet[O15],
    o16: Outlet[O16]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14],
        Outlet[O15],
        Outlet[O16]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14],
    o15: Outlet[O15],
    o16: Outlet[O16],
    o17: Outlet[O17]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14],
        Outlet[O15],
        Outlet[O16],
        Outlet[O17]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14],
    o15: Outlet[O15],
    o16: Outlet[O16],
    o17: Outlet[O17],
    o18: Outlet[O18]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14],
        Outlet[O15],
        Outlet[O16],
        Outlet[O17],
        Outlet[O18]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14],
    o15: Outlet[O15],
    o16: Outlet[O16],
    o17: Outlet[O17],
    o18: Outlet[O18],
    o19: Outlet[O19]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14],
        Outlet[O15],
        Outlet[O16],
        Outlet[O17],
        Outlet[O18],
        Outlet[O19]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18, o19)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14],
    o15: Outlet[O15],
    o16: Outlet[O16],
    o17: Outlet[O17],
    o18: Outlet[O18],
    o19: Outlet[O19],
    o20: Outlet[O20]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14],
        Outlet[O15],
        Outlet[O16],
        Outlet[O17],
        Outlet[O18],
        Outlet[O19],
        Outlet[O20]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18, o19, o20)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14],
    o15: Outlet[O15],
    o16: Outlet[O16],
    o17: Outlet[O17],
    o18: Outlet[O18],
    o19: Outlet[O19],
    o20: Outlet[O20],
    o21: Outlet[O21]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14],
        Outlet[O15],
        Outlet[O16],
        Outlet[O17],
        Outlet[O18],
        Outlet[O19],
        Outlet[O20],
        Outlet[O21]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18, o19, o20, o21)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }

  def to[O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21, O22](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6],
    o7: Outlet[O7],
    o8: Outlet[O8],
    o9: Outlet[O9],
    o10: Outlet[O10],
    o11: Outlet[O11],
    o12: Outlet[O12],
    o13: Outlet[O13],
    o14: Outlet[O14],
    o15: Outlet[O15],
    o16: Outlet[O16],
    o17: Outlet[O17],
    o18: Outlet[O18],
    o19: Outlet[O19],
    o20: Outlet[O20],
    o21: Outlet[O21],
    o22: Outlet[O22]
  )(using
    Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21, O22)
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = self.Out

      val inlets: Tuple.Map[In, Inlet] = self.inlets
      val outlets: (
        Outlet[O1],
        Outlet[O2],
        Outlet[O3],
        Outlet[O4],
        Outlet[O5],
        Outlet[O6],
        Outlet[O7],
        Outlet[O8],
        Outlet[O9],
        Outlet[O10],
        Outlet[O11],
        Outlet[O12],
        Outlet[O13],
        Outlet[O14],
        Outlet[O15],
        Outlet[O16],
        Outlet[O17],
        Outlet[O18],
        Outlet[O19],
        Outlet[O20],
        Outlet[O21],
        Outlet[O22]
      ) = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18, o19, o20, o21, o22)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor
    }
}
