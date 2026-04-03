package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.NotTuple
import zio.{Task, ZIO}
import scala.language.implicitConversions

trait FunctionConversions {
  inline given toZIO[A, I <: NonEmptyTuple, Out]: Conversion[A ?=> I => Out, A ?=> I => Task[Out]] with {
    transparent inline def apply(f: A ?=> I => Out): A ?=> I => Task[Out] = i => ZIO.succeed(f(i))
  }

  inline given f1[A, I: NotTuple, Out]: Conversion[A ?=> I => Out, A ?=> I *: EmptyTuple => Out] with {
    transparent inline def apply(f: A ?=> I => Out): A ?=> I *: EmptyTuple => Out = i => f(i.head)
  }

  inline given f2[A, I1: NotTuple, I2: NotTuple, Out]
    : Conversion[A ?=> (I1, I2) => Out, A ?=> I1 *: I2 *: EmptyTuple => Out] with {
    transparent inline def apply(f: A ?=> (I1, I2) => Out): A ?=> I1 *: I2 *: EmptyTuple => Out = i => f(i.head, i(1))
  }

  inline given f3[A, I1: NotTuple, I2: NotTuple, I3: NotTuple, Out]
    : Conversion[A ?=> (I1, I2, I3) => Out, A ?=> I1 *: I2 *: I3 *: EmptyTuple => Out] with {
    transparent inline def apply(f: A ?=> (I1, I2, I3) => Out): A ?=> I1 *: I2 *: I3 *: EmptyTuple => Out = i =>
      f(i.head, i(1), i(2))
  }

  inline given f4[A, I1: NotTuple, I2: NotTuple, I3: NotTuple, I4: NotTuple, Out]
    : Conversion[A ?=> (I1, I2, I3, I4) => Out, A ?=> I1 *: I2 *: I3 *: I4 *: EmptyTuple => Out] with {
    transparent inline def apply(f: A ?=> (I1, I2, I3, I4) => Out): A ?=> I1 *: I2 *: I3 *: I4 *: EmptyTuple => Out =
      i => f(i.head, i(1), i(2), i(3))
  }

  inline given f5[A, I1: NotTuple, I2: NotTuple, I3: NotTuple, I4: NotTuple, I5: NotTuple, Out]
    : Conversion[A ?=> (I1, I2, I3, I4, I5) => Out, A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: EmptyTuple => Out] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: EmptyTuple => Out = i => f(i.head, i(1), i(2), i(3), i(4))
  }

  inline given f6[A, I1: NotTuple, I2: NotTuple, I3: NotTuple, I4: NotTuple, I5: NotTuple, I6: NotTuple, Out]
    : Conversion[A ?=> (I1, I2, I3, I4, I5, I6) => Out, A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: EmptyTuple => Out]
  with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: EmptyTuple => Out = i => f(i.head, i(1), i(2), i(3), i(4), i(5))
  }

  inline given f7[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: EmptyTuple => Out = i =>
      f(i.head, i(1), i(2), i(3), i(4), i(5), i(6))
  }

  inline given f8[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: EmptyTuple => Out = i =>
      f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7))
  }

  inline given f9[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: EmptyTuple => Out = i =>
      f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8))
  }

  inline given f10[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: EmptyTuple => Out = i =>
      f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8), i(9))
  }

  inline given f11[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: EmptyTuple => Out = i =>
      f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8), i(9), i(10))
  }

  inline given f12[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: EmptyTuple => Out = i =>
      f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8), i(9), i(10), i(11))
  }

  inline given f13[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: EmptyTuple => Out = i =>
      f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8), i(9), i(10), i(11), i(12))
  }

  inline given f14[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: EmptyTuple => Out =
      i => f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8), i(9), i(10), i(11), i(12), i(13))
  }

  inline given f15[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    I15: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: EmptyTuple => Out =
      i => f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8), i(9), i(10), i(11), i(12), i(13), i(14))
  }

  inline given f16[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    I15: NotTuple,
    I16: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: EmptyTuple => Out =
      i => f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8), i(9), i(10), i(11), i(12), i(13), i(14), i(15))
  }

  inline given f17[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    I15: NotTuple,
    I16: NotTuple,
    I17: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: EmptyTuple => Out =
      i =>
        f(i.head, i(1), i(2), i(3), i(4), i(5), i(6), i(7), i(8), i(9), i(10), i(11), i(12), i(13), i(14), i(15), i(16))
  }

  inline given f18[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    I15: NotTuple,
    I16: NotTuple,
    I17: NotTuple,
    I18: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: EmptyTuple => Out =
      i =>
        f(
          i.head,
          i(1),
          i(2),
          i(3),
          i(4),
          i(5),
          i(6),
          i(7),
          i(8),
          i(9),
          i(10),
          i(11),
          i(12),
          i(13),
          i(14),
          i(15),
          i(16),
          i(17)
        )
  }

  inline given f19[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    I15: NotTuple,
    I16: NotTuple,
    I17: NotTuple,
    I18: NotTuple,
    I19: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: EmptyTuple => Out =
      i =>
        f(
          i.head,
          i(1),
          i(2),
          i(3),
          i(4),
          i(5),
          i(6),
          i(7),
          i(8),
          i(9),
          i(10),
          i(11),
          i(12),
          i(13),
          i(14),
          i(15),
          i(16),
          i(17),
          i(18)
        )
  }

  inline given f20[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    I15: NotTuple,
    I16: NotTuple,
    I17: NotTuple,
    I18: NotTuple,
    I19: NotTuple,
    I20: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: EmptyTuple => Out =
      i =>
        f(
          i.head,
          i(1),
          i(2),
          i(3),
          i(4),
          i(5),
          i(6),
          i(7),
          i(8),
          i(9),
          i(10),
          i(11),
          i(12),
          i(13),
          i(14),
          i(15),
          i(16),
          i(17),
          i(18),
          i(19)
        )
  }

  inline given f21[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    I15: NotTuple,
    I16: NotTuple,
    I17: NotTuple,
    I18: NotTuple,
    I19: NotTuple,
    I20: NotTuple,
    I21: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: I21 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: I21 *: EmptyTuple => Out =
      i =>
        f(
          i.head,
          i(1),
          i(2),
          i(3),
          i(4),
          i(5),
          i(6),
          i(7),
          i(8),
          i(9),
          i(10),
          i(11),
          i(12),
          i(13),
          i(14),
          i(15),
          i(16),
          i(17),
          i(18),
          i(19),
          i(20)
        )
  }

  inline given f22[
    A,
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple,
    I11: NotTuple,
    I12: NotTuple,
    I13: NotTuple,
    I14: NotTuple,
    I15: NotTuple,
    I16: NotTuple,
    I17: NotTuple,
    I18: NotTuple,
    I19: NotTuple,
    I20: NotTuple,
    I21: NotTuple,
    I22: NotTuple,
    Out
  ]: Conversion[
    A ?=> (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22) => Out,
    A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: I21 *: I22 *: EmptyTuple => Out
  ] with {
    transparent inline def apply(
      f: A ?=> (
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
      ) => Out
    ): A ?=> I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: I21 *: I22 *: EmptyTuple => Out =
      i =>
        f(
          i.head,
          i(1),
          i(2),
          i(3),
          i(4),
          i(5),
          i(6),
          i(7),
          i(8),
          i(9),
          i(10),
          i(11),
          i(12),
          i(13),
          i(14),
          i(15),
          i(16),
          i(17),
          i(18),
          i(19),
          i(20),
          i(21)
        )
  }
}
