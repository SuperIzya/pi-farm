


import sbt.{Def, _}
import Keys._
import xsbti.compile.CompileAnalysis

import scala.reflect.io.Directory


object ArduinoPlugin extends AutoPlugin {

  object autoImport {

    lazy val Arduino = config("arduino") extend Compile

    val build = taskKey[Seq[File]]("build arduino sketch")
    val upload = taskKey[Seq[File]]("build and upload arduino sketch")
    val devices = taskKey[Seq[String]]("collect all connected arduinos")
    val source = taskKey[File]("arduino source file")
    val buildIno = inputKey[Unit]("command to build arduino sketch")
    val uploadIno = inputKey[Unit]("command to upload arduino sketch")
    val actualRun = inputKey[Unit]("actual run input task")
  }

  import autoImport._

  lazy val arduinoPluginSettings: Seq[Def.Setting[_]] = Seq(
    source := ArduinoCmd.Source(
      (Compile / resourceDirectory).value / "arduino",
      streams.value
    ),
    watchSources +=  (Arduino / source).value,
    devices := ArduinoCmd.connect(streams.value),
    upload := ArduinoCmd.upload(
      (Arduino / source).value,
      (Arduino / devices).value,
      streams.value
    ),
    build := ArduinoCmd.build(
      (Arduino / source).value,
      streams.value
    ),
    Global / buildIno := {
      (Arduino / build).value
    },
    Global / uploadIno := {
      (Arduino / upload).value
    },
    Compile / run := Def.inputTaskDyn {
      import sbt.complete.Parsers.spaceDelimited
      val args = spaceDelimited("<args>").parsed
      Def.taskDyn {
        (Arduino / upload).value
        Def.task{
          Thread.sleep(1000)
        }.value
        (Arduino / actualRun).toTask(" " + args.mkString(" "))
      }
    }.evaluated,
    actualRun := Defaults.runTask(
      fullClasspath in Runtime,
      mainClass in (Compile, run),
      runner in (Compile, run)
    ).evaluated,
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

    def Source(resources: File, streams: TaskStreams): sbt.File = {
      val itSrc = Directory(resources)
        .walkFilter(_.hasExtension("ino"))
        .map(_.toFile)
        .take(1)
        .map(x => new File(x.toString))

      if (itSrc.hasNext) itSrc.next()
      else {
        streams.log.err(s"No ino files in folder $resources. Skipping")
        resources
      }
    }

    def connect(streams: TaskStreams): Seq[String] = {
      val log = streams.log
      log.info("Connecting to arduinos")
      new java.io.File("/dev/")
        .listFiles
        .toList
        .filter(_.getName.startsWith("ttyACM"))
        .map(_.getAbsolutePath)
    }

    def upload(sources: File,
               devices: Seq[String],
               stream: TaskStreams): Seq[File] = {

      val log = stream.log

      log.info(s"Uploading $sources sketch to ${devices.length} devices")

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


    def build(source: File,
              stream: TaskStreams): Seq[File] = {
      val log = stream.log
      log.info(s"Building $source sketch for arduino")
      val command = cmd("verify", source, stream.cacheDirectory)

      log.info(command)

      val res = command ! log
      if (res != 0) Seq.empty
      else hexFiles(stream)
    }
  }
}

