package org.pi.farm.model

import org.pi.farm.model.Types.ControllerId

import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.prelude.{Assertion, Subtype}

object Process {
  import Codec.given

  sealed trait Output {
    def message: String
    val stderr: Boolean = this match {
      case Output.Stdout(_) => false
      case Output.Stderr(_) => true
    }
    val stdout: Boolean = !stderr
  }

  object Output {
    final case class Stdout(message: String) extends Output
    final case class Stderr(message: String) extends Output
    def stdout(text: String): Output = Stdout(text)
    def stderr(text: String): Output = Stderr(text)

    given JsonCodec[Output] = DeriveJsonCodec.gen[Output]
  }

  enum Phase {
    case Idle, Preparing, Building, Uploading, Succeeded, Failed, Canceled, Timeout
  }
  private final val phaseNames  = Map(Phase.values.map(p => p -> p.toString.toLowerCase)*)
  private final val nameToPhase = phaseNames.view.map { case (k, v) => v -> k }.toMap
  given JsonCodec[Phase]        = JsonCodec[String].transformOrFail(
    s => nameToPhase.get(s.toLowerCase).toRight(s"Invalid phase: $s"),
    phaseNames(_)
  )

  case class Status(
    phase: Phase,
    controllerId: Option[ControllerId] = None,
    exitCode: Option[Int] = None,
    error: Option[String] = None
  )

  given JsonCodec[Status] = DeriveJsonCodec.gen[Status]
}
