package com.ilyak.pifarm.plugins.garden

import com.ilyak.pifarm.{Command, Units}

object Commands {

  case class Valve private (override val command: String) extends Command(command)

  object Valve {
    implicit val ou: Units[Valve] = new Units[Valve] {
      override val name: String = "valve"
    }
    private val _open = new Valve("open")
    private val _close = new Valve("close")
    def open: Valve = _open
    def close: Valve = _close
  }
}
