package com.ilyak.pifarm.sdk.configuration

sealed trait BlockType
object BlockType {
  case object Container extends BlockType
  case object Decider extends BlockType
}