package com.ilyak.pifarm.sdk

/***
  * Context of the current configuration.
  */
trait Context {
  val inputs: Seq[String]
  val outputs: Seq[String]
}
