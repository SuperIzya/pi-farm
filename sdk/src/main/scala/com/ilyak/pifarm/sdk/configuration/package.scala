package com.ilyak.pifarm.sdk

package object configuration {
  trait Input
  trait Output

  trait ControlConfiguration

  /***
    * General building block of the configuration
    */
  sealed trait GeneralBlock

  /***
    * Block that contains other blocks
    */
  case class Container(innerBlocks: Seq[GeneralBlock]) extends GeneralBlock

  /***
    * Describes data reception through inputs and transition to outputs
    */
  case class Decider(inputs: Seq[Input], outputs: Seq[Output]) extends GeneralBlock
}
