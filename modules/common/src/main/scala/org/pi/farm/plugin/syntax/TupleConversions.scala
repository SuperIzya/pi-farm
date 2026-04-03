package org.pi.farm.plugin.syntax

import org.pi.farm.plugin.{NotTuple, Inlet => I, Outlet => O}
import scala.language.implicitConversions

sealed trait TupleConversions[F[_]] {

  inline given [In: NotTuple]: Conversion[F[In], TF[F, In *: EmptyTuple]] with {
    inline def apply(f: F[In]): TF[F, In *: EmptyTuple] = f *: EmptyTuple
  }

  inline given [I1: NotTuple, I2: NotTuple]: Conversion[(F[I1], F[I2]), TF[F, I1 *: I2 *: EmptyTuple]] with {
    inline def apply(fs: (F[I1], F[I2])): TF[F, I1 *: I2 *: EmptyTuple] =
      fs._1 *: fs._2 *: EmptyTuple
  }

  inline given [I1: NotTuple, I2: NotTuple, I3: NotTuple]
    : Conversion[(F[I1], F[I2], F[I3]), TF[F, I1 *: I2 *: I3 *: EmptyTuple]] with {
    inline def apply(fs: (F[I1], F[I2], F[I3])): TF[F, I1 *: I2 *: I3 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: EmptyTuple
  }

  inline given [I1: NotTuple, I2: NotTuple, I3: NotTuple, I4: NotTuple]
    : Conversion[(F[I1], F[I2], F[I3], F[I4]), TF[F, I1 *: I2 *: I3 *: I4 *: EmptyTuple]] with {
    inline def apply(
      fs: (F[I1], F[I2], F[I3], F[I4])
    ): TF[F, I1 *: I2 *: I3 *: I4 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: EmptyTuple
  }

  inline given [I1: NotTuple, I2: NotTuple, I3: NotTuple, I4: NotTuple, I5: NotTuple]: Conversion[
    (F[I1], F[I2], F[I3], F[I4], F[I5]),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (F[I1], F[I2], F[I3], F[I4], F[I5])
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: EmptyTuple
  }

  inline given [I1: NotTuple, I2: NotTuple, I3: NotTuple, I4: NotTuple, I5: NotTuple, I6: NotTuple]: Conversion[
    (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6]),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6])
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: EmptyTuple
  }

  inline given [I1: NotTuple, I2: NotTuple, I3: NotTuple, I4: NotTuple, I5: NotTuple, I6: NotTuple, I7: NotTuple]
    : Conversion[
      (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6], F[I7]),
      TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: EmptyTuple]
    ] with {
    inline def apply(
      fs: (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6], F[I7])
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: EmptyTuple
  }

  inline given [F[
    _
  ], I1: NotTuple, I2: NotTuple, I3: NotTuple, I4: NotTuple, I5: NotTuple, I6: NotTuple, I7: NotTuple, I8: NotTuple]
    : Conversion[
      (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6], F[I7], F[I8]),
      TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: EmptyTuple]
    ] with {
    inline def apply(
      fs: (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6], F[I7], F[I8])
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: EmptyTuple
  }

  inline given [
    F[_],
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple
  ]: Conversion[
    (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6], F[I7], F[I8], F[I9]),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6], F[I7], F[I8], F[I9])
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: EmptyTuple
  }

  inline given [
    F[_],
    I1: NotTuple,
    I2: NotTuple,
    I3: NotTuple,
    I4: NotTuple,
    I5: NotTuple,
    I6: NotTuple,
    I7: NotTuple,
    I8: NotTuple,
    I9: NotTuple,
    I10: NotTuple
  ]: Conversion[
    (F[I1], F[I2], F[I3], F[I4], F[I5], F[I6], F[I7], F[I8], F[I9], F[I10]),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10]
      )
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I11: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11]
    ),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11]
      )
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I12: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12]
    ),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12]
      )
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I13: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13]
    ),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13]
      )
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I14: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14]
    ),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14]
      )
    ): TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: EmptyTuple] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I15: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14],
      F[I15]
    ),
    TF[F, I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: EmptyTuple]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14],
        F[I15]
      )
    ): TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: EmptyTuple
    ] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: fs._15 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I16: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14],
      F[I15],
      F[I16]
    ),
    TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: EmptyTuple
    ]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14],
        F[I15],
        F[I16]
      )
    ): TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: EmptyTuple
    ] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: fs._15 *: fs._16 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I17: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14],
      F[I15],
      F[I16],
      F[I17]
    ),
    TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: EmptyTuple
    ]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14],
        F[I15],
        F[I16],
        F[I17]
      )
    ): TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: EmptyTuple
    ] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: fs._15 *: fs._16 *: fs._17 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I18: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14],
      F[I15],
      F[I16],
      F[I17],
      F[I18]
    ),
    TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: EmptyTuple
    ]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14],
        F[I15],
        F[I16],
        F[I17],
        F[I18]
      )
    ): TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: EmptyTuple
    ] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: fs._15 *: fs._16 *: fs._17 *: fs._18 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I19: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14],
      F[I15],
      F[I16],
      F[I17],
      F[I18],
      F[I19]
    ),
    TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: EmptyTuple
    ]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14],
        F[I15],
        F[I16],
        F[I17],
        F[I18],
        F[I19]
      )
    ): TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: EmptyTuple
    ] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: fs._15 *: fs._16 *: fs._17 *: fs._18 *: fs._19 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I20: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14],
      F[I15],
      F[I16],
      F[I17],
      F[I18],
      F[I19],
      F[I20]
    ),
    TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: EmptyTuple
    ]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14],
        F[I15],
        F[I16],
        F[I17],
        F[I18],
        F[I19],
        F[I20]
      )
    ): TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: EmptyTuple
    ] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: fs._15 *: fs._16 *: fs._17 *: fs._18 *: fs._19 *: fs._20 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I21: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14],
      F[I15],
      F[I16],
      F[I17],
      F[I18],
      F[I19],
      F[I20],
      F[I21]
    ),
    TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: I21 *: EmptyTuple
    ]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14],
        F[I15],
        F[I16],
        F[I17],
        F[I18],
        F[I19],
        F[I20],
        F[I21]
      )
    ): TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: I21 *: EmptyTuple
    ] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: fs._15 *: fs._16 *: fs._17 *: fs._18 *: fs._19 *: fs._20 *: fs._21 *: EmptyTuple
  }

  inline given [
    F[_],
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
    I22: NotTuple
  ]: Conversion[
    (
      F[I1],
      F[I2],
      F[I3],
      F[I4],
      F[I5],
      F[I6],
      F[I7],
      F[I8],
      F[I9],
      F[I10],
      F[I11],
      F[I12],
      F[I13],
      F[I14],
      F[I15],
      F[I16],
      F[I17],
      F[I18],
      F[I19],
      F[I20],
      F[I21],
      F[I22]
    ),
    TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: I21 *: I22 *: EmptyTuple
    ]
  ] with {
    inline def apply(
      fs: (
        F[I1],
        F[I2],
        F[I3],
        F[I4],
        F[I5],
        F[I6],
        F[I7],
        F[I8],
        F[I9],
        F[I10],
        F[I11],
        F[I12],
        F[I13],
        F[I14],
        F[I15],
        F[I16],
        F[I17],
        F[I18],
        F[I19],
        F[I20],
        F[I21],
        F[I22]
      )
    ): TF[
      F,
      I1 *: I2 *: I3 *: I4 *: I5 *: I6 *: I7 *: I8 *: I9 *: I10 *: I11 *: I12 *: I13 *: I14 *: I15 *: I16 *: I17 *: I18 *: I19 *: I20 *: I21 *: I22 *: EmptyTuple
    ] =
      fs._1 *: fs._2 *: fs._3 *: fs._4 *: fs._5 *: fs._6 *: fs._7 *: fs._8 *: fs._9 *: fs._10 *: fs._11 *: fs._12 *: fs._13 *: fs._14 *: fs._15 *: fs._16 *: fs._17 *: fs._18 *: fs._19 *: fs._20 *: fs._21 *: fs._22 *: EmptyTuple
  }

}

object TupleConversions {
  object Inlet  extends TupleConversions[I]
  object Outlet extends TupleConversions[O]
}
