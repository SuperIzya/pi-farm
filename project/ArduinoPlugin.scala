


import sbt.{Def, _}
import Keys._

import scala.reflect.io.Directory


object ArduinoPlugin extends AutoPlugin {

  object autoImport {

    lazy val Arduino = config("arduino") extend Compile

    val build = taskKey[Seq[File]]("build arduino sketch")
    val upload = taskKey[Seq[File]]("build and upload arduino sketch")
    val devices = taskKey[Seq[String]]("collect all connected arduinos")
    val source = taskKey[File]("arduino source file")
  }

  import autoImport._

  lazy val arduinoPluginSettings: Seq[Def.Setting[_]] = Seq(
    source := ArduinoCmd.Source(
      (Compile / resourceDirectory).value / "arduino",
      streams.value
    ),
    watchSources +=  (Arduino / source).value,
    devices := ArduinoCmd.Connect(streams.value),
    upload := ArduinoCmd.Upload(
      (Arduino / source).value,
      (Arduino / devices).value,
      streams.value
    ),
    build := ArduinoCmd.Build(
      (Arduino / source).value,
      streams.value
    ),
    Compile / sourceGenerators += (Arduino / build).taskValue,

    Compile / run := {
      (Arduino / build).value
      (Compile / run).evaluated
    }
  )

  override def projectSettings: Seq[Def.Setting[_]] = inConfig(Arduino)(arduinoPluginSettings)

  private object ArduinoCmd {

    import scala.sys.process._

    private def cmd(command: String, source: File, target: File, port: Option[String] = None): String =
      port match {
        case None => s"arduino --verbose --$command $source"
        case Some(p) => s"arduino --port $p --verbose --$command $source"
      }

    private def hexFiles(stream: TaskStreams): Seq[File] =
      Directory(stream.cacheDirectory)
        .walkFilter(_.hasExtension("hex"))
        .map(f => new File(f.toFile.toString))
        .toSeq

    object Source {
      def apply(resources: File, streams: TaskStreams): sbt.File = {
        val itSrc = Directory(resources)
          .walkFilter(_.hasExtension("ino"))
          .map(_.toFile)
          .take(1)
          .map(x => new File(x.toString))

        if(itSrc.hasNext) itSrc.next()
        else {
          streams.log.err(s"No ino files in folder $resources. Skipping")
          resources
        }
      }
    }

    object Connect {
      def apply(streams: TaskStreams): Seq[String] = {
        streams.log("Connecting to arduino")
        Directory("/dev/")
          .files
          .map(_.toString)
          .filter(_.startsWith("/dev/ttyACM"))
          .map(s => {
            streams.log(s"Device found - $s")
            s
          })
          .toSeq
      }
    }

    object Upload {
      def apply(sources: File,
                devices: Seq[String],
                stream: TaskStreams): Seq[File] = {

        val log = stream.log

        def prefix(port: String) =
          cmd("upload", sources, stream.cacheDirectory, Some(port))

        val res = devices.map(prefix)
          .map(s => {
            log.info(s)
            s ! log
          })
          .forall(_ == 0)

        if (!res) Seq.empty
        else hexFiles(stream)
      }
    }

    object Build {
      def apply(source: File,
                stream: TaskStreams): Seq[File] = {
        val log = stream.log

        val command = cmd("verify", source, stream.cacheDirectory)

        log.info(command)

        val res = command ! log
        if(res != 0) Seq.empty
        else hexFiles(stream)
      }
    }
  }
}

