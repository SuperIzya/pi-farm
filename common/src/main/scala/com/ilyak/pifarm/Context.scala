package com.ilyak.pifarm

/***
  * Context of the process of current parsing of configuration.
  */
trait Context {
  def parseInner()
}
