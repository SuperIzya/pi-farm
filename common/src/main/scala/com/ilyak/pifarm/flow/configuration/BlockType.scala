package com.ilyak.pifarm.flow.configuration

/***
  * Type of the building block of the control configuration.
  * It can be either Container, that contains other blocks,
  * or Automaton that produces output based on streamed-in output.
  */
sealed trait BlockType
object BlockType {
  case object Container extends BlockType
  case object Automaton extends BlockType
}
