package com.ilyak.pifarm.conversion
/*

trait GetWithConversion[T] {
  type In
  val conversion: Conversion.Aux[In, T]
  val getter: Getter[In]
}

object GetWithConversion {
  type Aux[T, I] = GetWithConversion[T] { type In = I }


  def pair[T, I](g: Getter[I])(implicit conv: Conversion.Aux[I, T]): (String, Aux[T, I]) = g.typeName -> new GetWithConversion[T] {
    type In = I
    override val conversion: Conversion.Aux[I, T] = conv
    override val getter: Getter[I] = g
  }
}
*/
