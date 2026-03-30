package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.{Inlet, NotTuple, Processor}
import zio.{ZIO, Task}
import zio.json.JsonCodec

transparent trait Inlets { self: Processor =>
  extension [In](inlet: Inlet[In]) {
    inline def to[Out](
      process: In => ParamsType ?=> Out
    )(using NotTuple[In]): Outlets[Tuple1[In], Out, ParamsType, Any, Throwable] =
      new Outlets[Tuple1[In], Out, ParamsType, Any, Throwable] {
        def inlets = Tuple1(inlet)

        def processor: In => ParamsType ?=> Task[Out] = i => params ?=> ZIO.attempt(process(i))
      }

    inline def to[Out, R, E](
      process: In => ParamsType ?=> ZIO[R, E, Out]
    )(using NotTuple[In]): Outlets[Tuple1[In], Out, ParamsType, R, E] =
      new Outlets[Tuple1[In], Out, ParamsType, R, E] {
        def inlets                                         = Tuple1(inlet)
        def processor: In => ParamsType ?=> ZIO[R, E, Out] = i => params ?=> process(i)
      }
  }

  extension [In <: Tuple](inlet: Tuple.Map[In, Inlet]) {
    inline def to[Out](
      process: In => ParamsType ?=> Out
    )(using InletsSetter[In]): Outlets[In, Out, ParamsType, Any, Throwable] =
      new Outlets[In, Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: In => ParamsType ?=> Task[Out] = i => params ?=> ZIO.attempt(process(i))
      }

    inline def to[Out, R, E](
      process: In => ParamsType ?=> ZIO[R, E, Out]
    )(using InletsSetter[In]): Outlets[In, Out, ParamsType, R, E] =
      new Outlets[In, Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: In => ParamsType ?=> ZIO[R, E, Out] = i => params ?=> process(i)
      }
  }

  extension [I1, I2](inlet: (Inlet[I1], Inlet[I2])) {
    inline def to[Out](
      process: (I1, I2) => ParamsType ?=> Out
    )(using InletsSetter[(I1, I2)]): Outlets[(I1, I2), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2), Out, ParamsType, Any, Throwable] {
        val inlets = inlet

        def processor: ((I1, I2)) => ParamsType ?=> Task[Out] = i => params ?=> ZIO.attempt(process(i._1, i._2))
      }

    inline def to[Out, R, E](
      process: (I1, I2) => ParamsType ?=> ZIO[R, E, Out]
    )(using InletsSetter[(I1, I2)]): Outlets[(I1, I2), Out, ParamsType, R, E] =
      new Outlets[(I1, I2), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2)) => ParamsType ?=> ZIO[R, E, Out] = i => params ?=> process(i._1, i._2)
      }
  }

  extension [I1, I2, I3](inlet: (Inlet[I1], Inlet[I2], Inlet[I3])) {
    inline def to[Out](
      process: (I1, I2, I3) => ParamsType ?=> Out
    )(using InletsSetter[(I1, I2, I3)]): Outlets[(I1, I2, I3), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3) => ParamsType ?=> ZIO[R, E, Out]
    )(using InletsSetter[(I1, I2, I3)]): Outlets[(I1, I2, I3), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3)) => ParamsType ?=> ZIO[R, E, Out] = i => params ?=> process(i._1, i._2, i._3)
      }
  }

  extension [I1, I2, I3, I4](inlet: (Inlet[I1], Inlet[I2], Inlet[I3], Inlet[I4])) {
    inline def to[Out](
      process: (I1, I2, I3, I4) => ParamsType ?=> Out
    )(using InletsSetter[(I1, I2, I3, I4)]): Outlets[(I1, I2, I3, I4), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4) => ParamsType ?=> ZIO[R, E, Out]
    )(using InletsSetter[(I1, I2, I3, I4)]): Outlets[(I1, I2, I3, I4), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4)
      }
  }

  extension [I1, I2, I3, I4, I5](inlet: (Inlet[I1], Inlet[I2], Inlet[I3], Inlet[I4], Inlet[I5])) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5) => ParamsType ?=> Out
    )(using InletsSetter[(I1, I2, I3, I4, I5)]): Outlets[(I1, I2, I3, I4, I5), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4, i._5))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5) => ParamsType ?=> ZIO[R, E, Out]
    )(using InletsSetter[(I1, I2, I3, I4, I5)]): Outlets[(I1, I2, I3, I4, I5), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4, i._5)
      }
  }

  extension [I1, I2, I3, I4, I5, I6](inlet: (Inlet[I1], Inlet[I2], Inlet[I3], Inlet[I4], Inlet[I5], Inlet[I6])) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6)]
    ): Outlets[(I1, I2, I3, I4, I5, I6), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4, i._5, i._6))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6) => ParamsType ?=> ZIO[R, E, Out]
    )(using InletsSetter[(I1, I2, I3, I4, I5, I6)]): Outlets[(I1, I2, I3, I4, I5, I6), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4, i._5, i._6)
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7](
    inlet: (Inlet[I1], Inlet[I2], Inlet[I3], Inlet[I4], Inlet[I5], Inlet[I6], Inlet[I7])
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4, i._5, i._6, i._7))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7) => ParamsType ?=> ZIO[R, E, Out]
    )(using InletsSetter[(I1, I2, I3, I4, I5, I6, I7)]): Outlets[(I1, I2, I3, I4, I5, I6, I7), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4, i._5, i._6, i._7)
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8](
    inlet: (Inlet[I1], Inlet[I2], Inlet[I3], Inlet[I4], Inlet[I5], Inlet[I6], Inlet[I7], Inlet[I8])
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8)
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9](
    inlet: (Inlet[I1], Inlet[I2], Inlet[I3], Inlet[I4], Inlet[I5], Inlet[I6], Inlet[I7], Inlet[I8], Inlet[I9])
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8, i._9))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8, i._9)
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8, i._9, i._10))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8, i._9, i._10)
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8, i._9, i._10, i._11))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8, i._9, i._10, i._11)
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12)) => ParamsType ?=> Task[Out] = i =>
          params ?=> ZIO.attempt(process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8, i._9, i._10, i._11, i._12))
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=> process(i._1, i._2, i._3, i._4, i._5, i._6, i._7, i._8, i._9, i._10, i._11, i._12)
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13)) => ParamsType ?=> Task[Out] = i =>
          params ?=>
            ZIO.attempt(
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13
              )
            )
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13)) => ParamsType ?=> ZIO[R, E, Out] =
          i =>
            params ?=>
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13
              )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14), Out, ParamsType, Any, Throwable] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14), Out, ParamsType, Any, Throwable] {
        def inlets = inlet

        def processor: ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14)) => ParamsType ?=> Task[Out] =
          i =>
            params ?=>
              ZIO.attempt(
                process(
                  i._1,
                  i._2,
                  i._3,
                  i._4,
                  i._5,
                  i._6,
                  i._7,
                  i._8,
                  i._9,
                  i._10,
                  i._11,
                  i._12,
                  i._13,
                  i._14
                )
              )
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor
          : ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=>
            process(
              i._1,
              i._2,
              i._3,
              i._4,
              i._5,
              i._6,
              i._7,
              i._8,
              i._9,
              i._10,
              i._11,
              i._12,
              i._13,
              i._14
            )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14],
      Inlet[I15]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15),
      Out,
      ParamsType,
      Any,
      Throwable
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15),
        Out,
        ParamsType,
        Any,
        Throwable
      ] {
        def inlets = inlet

        def processor
          : ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15)) => ParamsType ?=> Task[Out] = i =>
          params ?=>
            ZIO.attempt(
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13,
                i._14,
                i._15
              )
            )
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor
          : ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15)) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=>
            process(
              i._1,
              i._2,
              i._3,
              i._4,
              i._5,
              i._6,
              i._7,
              i._8,
              i._9,
              i._10,
              i._11,
              i._12,
              i._13,
              i._14,
              i._15
            )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14],
      Inlet[I15],
      Inlet[I16]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16),
      Out,
      ParamsType,
      Any,
      Throwable
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16),
        Out,
        ParamsType,
        Any,
        Throwable
      ] {
        def inlets = inlet

        def processor
          : ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16)) => ParamsType ?=> Task[Out] = i =>
          params ?=>
            ZIO.attempt(
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13,
                i._14,
                i._15,
                i._16
              )
            )
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16)]
    ): Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16), Out, ParamsType, R, E] =
      new Outlets[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16), Out, ParamsType, R, E] {
        def inlets = inlet

        def processor
          : ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16)) => ParamsType ?=> ZIO[R, E, Out] =
          i =>
            params ?=>
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13,
                i._14,
                i._15,
                i._16
              )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14],
      Inlet[I15],
      Inlet[I16],
      Inlet[I17]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17),
      Out,
      ParamsType,
      Any,
      Throwable
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17),
        Out,
        ParamsType,
        Any,
        Throwable
      ] {
        def inlets = inlet

        def processor
          : ((I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17)) => ParamsType ?=> Task[Out] =
          i =>
            params ?=>
              ZIO.attempt(
                process(
                  i._1,
                  i._2,
                  i._3,
                  i._4,
                  i._5,
                  i._6,
                  i._7,
                  i._8,
                  i._9,
                  i._10,
                  i._11,
                  i._12,
                  i._13,
                  i._14,
                  i._15,
                  i._16,
                  i._17
                )
              )
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17) => ParamsType ?=> ZIO[
        R,
        E,
        Out
      ]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17),
      Out,
      ParamsType,
      R,
      E
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17),
        Out,
        ParamsType,
        R,
        E
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17)
        ) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=>
            process(
              i._1,
              i._2,
              i._3,
              i._4,
              i._5,
              i._6,
              i._7,
              i._8,
              i._9,
              i._10,
              i._11,
              i._12,
              i._13,
              i._14,
              i._15,
              i._16,
              i._17
            )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14],
      Inlet[I15],
      Inlet[I16],
      Inlet[I17],
      Inlet[I18]
    )
  ) {
    inline def to[Out](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18),
      Out,
      ParamsType,
      Any,
      Throwable
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18),
        Out,
        ParamsType,
        Any,
        Throwable
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18)
        ) => ParamsType ?=> Task[Out] = i =>
          params ?=>
            ZIO.attempt(
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13,
                i._14,
                i._15,
                i._16,
                i._17,
                i._18
              )
            )
      }

    inline def to[Out, R, E](
      process: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18) => ParamsType ?=> ZIO[
        R,
        E,
        Out
      ]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18),
      Out,
      ParamsType,
      R,
      E
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18),
        Out,
        ParamsType,
        R,
        E
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18)
        ) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=>
            process(
              i._1,
              i._2,
              i._3,
              i._4,
              i._5,
              i._6,
              i._7,
              i._8,
              i._9,
              i._10,
              i._11,
              i._12,
              i._13,
              i._14,
              i._15,
              i._16,
              i._17,
              i._18
            )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14],
      Inlet[I15],
      Inlet[I16],
      Inlet[I17],
      Inlet[I18],
      Inlet[I19]
    )
  ) {
    inline def to[Out](
      process: (
        I1,
        I2,
        I3,
        I4,
        I5,
        I6,
        I7,
        I8,
        I9,
        I10,
        I11,
        I12,
        I13,
        I14,
        I15,
        I16,
        I17,
        I18,
        I19
      ) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19),
      Out,
      ParamsType,
      Any,
      Throwable
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19),
        Out,
        ParamsType,
        Any,
        Throwable
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19)
        ) => ParamsType ?=> Task[Out] = i =>
          params ?=>
            ZIO.attempt(
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13,
                i._14,
                i._15,
                i._16,
                i._17,
                i._18,
                i._19
              )
            )
      }

    inline def to[Out, R, E](
      process: (
        I1,
        I2,
        I3,
        I4,
        I5,
        I6,
        I7,
        I8,
        I9,
        I10,
        I11,
        I12,
        I13,
        I14,
        I15,
        I16,
        I17,
        I18,
        I19
      ) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19),
      Out,
      ParamsType,
      R,
      E
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19),
        Out,
        ParamsType,
        R,
        E
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19)
        ) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=>
            process(
              i._1,
              i._2,
              i._3,
              i._4,
              i._5,
              i._6,
              i._7,
              i._8,
              i._9,
              i._10,
              i._11,
              i._12,
              i._13,
              i._14,
              i._15,
              i._16,
              i._17,
              i._18,
              i._19
            )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14],
      Inlet[I15],
      Inlet[I16],
      Inlet[I17],
      Inlet[I18],
      Inlet[I19],
      Inlet[I20]
    )
  ) {
    inline def to[Out](
      process: (
        I1,
        I2,
        I3,
        I4,
        I5,
        I6,
        I7,
        I8,
        I9,
        I10,
        I11,
        I12,
        I13,
        I14,
        I15,
        I16,
        I17,
        I18,
        I19,
        I20
      ) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20),
      Out,
      ParamsType,
      Any,
      Throwable
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20),
        Out,
        ParamsType,
        Any,
        Throwable
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20)
        ) => ParamsType ?=> Task[Out] = i =>
          params ?=>
            ZIO.attempt(
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13,
                i._14,
                i._15,
                i._16,
                i._17,
                i._18,
                i._19,
                i._20
              )
            )
      }

    inline def to[Out, R, E](
      process: (
        I1,
        I2,
        I3,
        I4,
        I5,
        I6,
        I7,
        I8,
        I9,
        I10,
        I11,
        I12,
        I13,
        I14,
        I15,
        I16,
        I17,
        I18,
        I19,
        I20
      ) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20),
      Out,
      ParamsType,
      R,
      E
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20),
        Out,
        ParamsType,
        R,
        E
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20)
        ) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=>
            process(
              i._1,
              i._2,
              i._3,
              i._4,
              i._5,
              i._6,
              i._7,
              i._8,
              i._9,
              i._10,
              i._11,
              i._12,
              i._13,
              i._14,
              i._15,
              i._16,
              i._17,
              i._18,
              i._19,
              i._20
            )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14],
      Inlet[I15],
      Inlet[I16],
      Inlet[I17],
      Inlet[I18],
      Inlet[I19],
      Inlet[I20],
      Inlet[I21]
    )
  ) {
    inline def to[Out](
      process: (
        I1,
        I2,
        I3,
        I4,
        I5,
        I6,
        I7,
        I8,
        I9,
        I10,
        I11,
        I12,
        I13,
        I14,
        I15,
        I16,
        I17,
        I18,
        I19,
        I20,
        I21
      ) => ParamsType ?=> Out
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21),
      Out,
      ParamsType,
      Any,
      Throwable
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21),
        Out,
        ParamsType,
        Any,
        Throwable
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21)
        ) => ParamsType ?=> Task[Out] = i =>
          params ?=>
            ZIO.attempt(
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13,
                i._14,
                i._15,
                i._16,
                i._17,
                i._18,
                i._19,
                i._20,
                i._21
              )
            )
      }

    inline def to[Out, R, E](
      process: (
        I1,
        I2,
        I3,
        I4,
        I5,
        I6,
        I7,
        I8,
        I9,
        I10,
        I11,
        I12,
        I13,
        I14,
        I15,
        I16,
        I17,
        I18,
        I19,
        I20,
        I21
      ) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[(I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21)]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21),
      Out,
      ParamsType,
      R,
      E
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21),
        Out,
        ParamsType,
        R,
        E
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21)
        ) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=>
            process(
              i._1,
              i._2,
              i._3,
              i._4,
              i._5,
              i._6,
              i._7,
              i._8,
              i._9,
              i._10,
              i._11,
              i._12,
              i._13,
              i._14,
              i._15,
              i._16,
              i._17,
              i._18,
              i._19,
              i._20,
              i._21
            )
      }
  }

  extension [I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22](
    inlet: (
      Inlet[I1],
      Inlet[I2],
      Inlet[I3],
      Inlet[I4],
      Inlet[I5],
      Inlet[I6],
      Inlet[I7],
      Inlet[I8],
      Inlet[I9],
      Inlet[I10],
      Inlet[I11],
      Inlet[I12],
      Inlet[I13],
      Inlet[I14],
      Inlet[I15],
      Inlet[I16],
      Inlet[I17],
      Inlet[I18],
      Inlet[I19],
      Inlet[I20],
      Inlet[I21],
      Inlet[I22]
    )
  ) {
    inline def to[Out](
      process: (
        I1,
        I2,
        I3,
        I4,
        I5,
        I6,
        I7,
        I8,
        I9,
        I10,
        I11,
        I12,
        I13,
        I14,
        I15,
        I16,
        I17,
        I18,
        I19,
        I20,
        I21,
        I22
      ) => ParamsType ?=> Out
    )(using
      InletsSetter[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22)
      ]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22),
      Out,
      ParamsType,
      Any,
      Throwable
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22),
        Out,
        ParamsType,
        Any,
        Throwable
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22)
        ) => ParamsType ?=> Task[Out] = i =>
          params ?=>
            ZIO.attempt(
              process(
                i._1,
                i._2,
                i._3,
                i._4,
                i._5,
                i._6,
                i._7,
                i._8,
                i._9,
                i._10,
                i._11,
                i._12,
                i._13,
                i._14,
                i._15,
                i._16,
                i._17,
                i._18,
                i._19,
                i._20,
                i._21,
                i._22
              )
            )
      }

    inline def to[Out, R, E](
      process: (
        I1,
        I2,
        I3,
        I4,
        I5,
        I6,
        I7,
        I8,
        I9,
        I10,
        I11,
        I12,
        I13,
        I14,
        I15,
        I16,
        I17,
        I18,
        I19,
        I20,
        I21,
        I22
      ) => ParamsType ?=> ZIO[R, E, Out]
    )(using
      InletsSetter[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22)
      ]
    ): Outlets[
      (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22),
      Out,
      ParamsType,
      R,
      E
    ] =
      new Outlets[
        (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22),
        Out,
        ParamsType,
        R,
        E
      ] {
        def inlets = inlet

        def processor: (
          (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22)
        ) => ParamsType ?=> ZIO[R, E, Out] = i =>
          params ?=>
            process(
              i._1,
              i._2,
              i._3,
              i._4,
              i._5,
              i._6,
              i._7,
              i._8,
              i._9,
              i._10,
              i._11,
              i._12,
              i._13,
              i._14,
              i._15,
              i._16,
              i._17,
              i._18,
              i._19,
              i._20,
              i._21,
              i._22
            )
      }
  }
}
