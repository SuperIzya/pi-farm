package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.{Inlet, NotTuple, Outlet}
import org.pi.farm.runtime.Environment
import zio.ZIO
import zio.json.JsonCodec

transparent trait Outlets[In <: NonEmptyTuple, Out, ParamsType, R >: Environment, E <: Throwable](using
  codec: JsonCodec[ParamsType],
  valuesSetter: InletsSetter[In]
) { self =>
  def inlets: TInlets[In]

  def processor: In => ParamsType ?=> ZIO[R, E, Out]

  def to(
    outlet: Outlet[Out]
  )(using NotTuple[Out], OutletsSetter[Tuple1[Out]]): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = Tuple1[self.Out]

      val inlets: TInlets[In]               = self.inlets
      val outlets: TOutlets[Out]            = Tuple1(outlet)
      val valuesSetter: InletsSetter[In]    = self.valuesSetter
      val resultBuilder: OutletsSetter[Out] = OutletsSetter[Out]

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor andThen (_.map(Tuple1(_)))
    }

  def to[O <: NonEmptyTuple](
    outlet: O
  )(using
    ev: Out =:= InversTOutlets[O],
    ev2: O =:= TOutlets[Out],
    s: OutletsSetter[InversTOutlets[O]]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = InversTOutlets[O]

      val inlets: TInlets[In]               = self.inlets
      val outlets: TOutlets[Out]            = ev.substituteCo(ev2(outlet))
      val valuesSetter: InletsSetter[In]    = self.valuesSetter
      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev(_)))
    }

  def to[O1, O2](o1: Outlet[O1], o2: Outlet[O2])(using
    ev: Out =:= (O1, O2),
    s: OutletsSetter[(O1, O2)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2)

      val inlets: TInlets[In]               = self.inlets
      val outlets: TOutlets[Out]            = (o1, o2)
      val valuesSetter: InletsSetter[In]    = self.valuesSetter
      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
    }

  def to[O1, O2, O3](o1: Outlet[O1], o2: Outlet[O2], o3: Outlet[O3])(using
    ev: Out =:= (O1, O2, O3),
    s: OutletsSetter[(O1, O2, O3)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3)

      val inlets: TInlets[In]               = self.inlets
      val outlets: TOutlets[Out]            = (o1, o2, o3)
      val valuesSetter: InletsSetter[In]    = self.valuesSetter
      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
    }

  def to[O1, O2, O3, O4](o1: Outlet[O1], o2: Outlet[O2], o3: Outlet[O3], o4: Outlet[O4])(using
    ev: Out =:= (O1, O2, O3, O4),
    s: OutletsSetter[(O1, O2, O3, O4)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4)

      val inlets: Tuple.Map[In, Inlet]      = self.inlets
      val outlets: TOutlets[Out]            = (o1, o2, o3, o4)
      val valuesSetter: InletsSetter[In]    = self.valuesSetter
      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
    }

  def to[O1, O2, O3, O4, O5](o1: Outlet[O1], o2: Outlet[O2], o3: Outlet[O3], o4: Outlet[O4], o5: Outlet[O5])(using
    ev: Out =:= (O1, O2, O3, O4, O5),
    s: OutletsSetter[(O1, O2, O3, O4, O5)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5)

      val inlets: Tuple.Map[In, Inlet]      = self.inlets
      val outlets: TOutlets[Out]            = (o1, o2, o3, o4, o5)
      val valuesSetter: InletsSetter[In]    = self.valuesSetter
      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
    }

  def to[O1, O2, O3, O4, O5, O6](
    o1: Outlet[O1],
    o2: Outlet[O2],
    o3: Outlet[O3],
    o4: Outlet[O4],
    o5: Outlet[O5],
    o6: Outlet[O6]
  )(using
    ev: Out =:= (O1, O2, O3, O4, O5, O6),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6)

      val inlets: Tuple.Map[In, Inlet] = self.inlets

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6)

      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9)

      val inlets: Tuple.Map[In, Inlet]      = self.inlets
      val valuesSetter: InletsSetter[In]    = self.valuesSetter
      val resultBuilder: OutletsSetter[Out] = s

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9)

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out]            = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10)
      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] = (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] =
        (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18, o19)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] =
        (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18, o19, o20)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21),
    s: OutletsSetter[(O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21)]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] =
        (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18, o19, o20, o21)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
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
    ev: Out =:= (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21, O22),
    s: OutletsSetter[
      (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21, O22)
    ]
  ): ConfigurableProcessor[ParamsType] =
    new ConfigurableProcessor[ParamsType] {
      type In  = self.In
      type R   = self.R
      type E   = self.E
      type Out = (O1, O2, O3, O4, O5, O6, O7, O8, O9, O10, O11, O12, O13, O14, O15, O16, O17, O18, O19, O20, O21, O22)

      val inlets: Tuple.Map[In, Inlet]   = self.inlets
      val valuesSetter: InletsSetter[In] = self.valuesSetter

      val outlets: TOutlets[Out] =
        (o1, o2, o3, o4, o5, o6, o7, o8, o9, o10, o11, o12, o13, o14, o15, o16, o17, o18, o19, o20, o21, o22)

      val resultBuilder: OutletsSetter[Out] = s

      def processor: In => ParamsType ?=> ZIO[R, E, Out] = self.processor.andThen(_.map(ev.flip(_)))
    }
}
