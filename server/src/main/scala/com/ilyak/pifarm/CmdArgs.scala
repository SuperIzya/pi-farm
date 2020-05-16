package com.ilyak.pifarm
import scopt.OParser

case class CmdArgs(openBrowser: Boolean = false)

object CmdArgs {
  val builder = OParser.builder[CmdArgs]
  val parser = {
    import builder._
    OParser.sequence(
      programName("pi-farm"),
      opt[Boolean]('b', "browser")
        .action((b, a) => a.copy(openBrowser = b))
        .text("Automatically open browser on start. Default - false.")
    )
  }

  def parseArgs(args: List[String]): CmdArgs = OParser.parse(parser, args, CmdArgs()) getOrElse CmdArgs()
}
