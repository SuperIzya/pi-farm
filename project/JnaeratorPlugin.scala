// from com.timcharper.sbt

import java.io.File

import sbt.Keys._
import sbt.{Compile, _}

object JnaeratorPlugin extends AutoPlugin {

  sealed trait Runtime

  object Runtime {

    case object JNA extends Runtime

    case object BridJ extends Runtime

  }

  case class JnaeratorTarget(headerFile: File,
                             packageName: String,
                             libraryName: String,
                             extraArgs: Seq[String] = Seq.empty)

  object autoImport {

    lazy val Jnaerator = config("jnaerator") extend Compile

    val jnaeratorGenerate = taskKey[Seq[File]]("generate jna")

    val jnaeratorTargets = settingKey[Seq[JnaeratorTarget]]("jnaerator targets")
    val jnaeratorRuntime = settingKey[Runtime]("which runtime to use")
    val jnaVersion = settingKey[String]("jna version")
    val bridjVersion = settingKey[String]("bridJ version")
    val jnaerate = inputKey[Unit]("run jnaerator")
  }

  import autoImport._

  override lazy val globalSettings = Seq(
    jnaVersion := "4.2.1",
    bridjVersion := "0.7.0",
    jnaeratorRuntime := Runtime.BridJ,
    jnaeratorTargets := Seq.empty
  )

  lazy val jnaeratorSettings: Seq[Setting[_]] = Seq(
    sourceDirectory := (Compile / sourceDirectory) {
      _ / "native"
    }.value,
    sourceDirectories := (Compile / sourceDirectory) {
      _ :: Nil
    }.value,
    sourceManaged := (Compile / sourceManaged) {
      _ / "jnaerator_interfaces"
    }.value,

    cleanFiles += (Jnaerator / sourceManaged).value,

    watchSources ++= (Jnaerator / jnaeratorTargets) {
      _.map(_.headerFile)
    }.value,

    jnaeratorGenerate := Def.task {
      JnaeratorCmd(
        (Jnaerator / jnaeratorTargets).value,
        streams.value,
        (Jnaerator / jnaeratorRuntime).value,
        (Jnaerator / sourceManaged).value
      )
    }.value,

    Compile / managedSourceDirectories += (Jnaerator / sourceManaged).value,
    Compile / sourceGenerators += (Jnaerator / jnaeratorGenerate).taskValue,
    Global / (Compile / libraryDependencies) += {
      (Jnaerator / jnaeratorRuntime).value match {
        case Runtime.JNA => "net.java.dev.jna" % "jna" % (Jnaerator / jnaVersion).value
        case Runtime.BridJ => "com.nativelibs4java" % "bridj" % (Jnaerator / bridjVersion).value
      }
    }
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Jnaerator)(jnaeratorSettings) ++ Seq(
    jnaerate := Def.inputTask {

      (Jnaerator / jnaeratorGenerate).value
    }.evaluated
  )

  private object JnaeratorCmd {
    def apply(targets: Seq[JnaeratorTarget],
              s: TaskStreams,
              runtime: Runtime,
              outputPath: File
             ): Seq[File] = {
      val log: (=> String) => Unit = s.log.log(Level.Info, _)

      val targetId = "c" + targets.toList.map { target =>
        (target, runtime, outputPath)
      }.hashCode

      log(s"${targets.size} jnaerator targets:")
      targets.map(_.headerFile.getName).foreach(log(_))
      val ff = FileFunction.cached(
        s.cacheDirectory / "jnaerator" / targetId,
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists
      )(_)
      val cachedCompile = ff { _: Set[File] =>
        IO.delete(outputPath)
        outputPath.mkdirs()

        log(targets.size.toString)
        targets.flatMap { target =>
          // java -jar bin/jnaerator.jar -package com.package.name -library libName libraries/libName.h -o src/main/java -mode Directory -f -scalaStructSetters
          val args = List(
            "-package", target.packageName,
            "-library", target.libraryName,
            "-o", outputPath.getCanonicalPath,
            "-mode", "Directory",
            "-f",
            "-scalaStructSetters"
          ) ++ target.extraArgs ++ Seq(target.headerFile.getCanonicalPath)

          s.log.info(s"(${target.headerFile.getName}) Running JNAerator with args ${args.mkString(" ")}")
          try {
            com.ochafik.lang.jnaerator.JNAerator.main(args.toArray)
          } catch {
            case e: Exception =>
              throw new RuntimeException(s"error occured while running jnaerator: ${e.getMessage}", e)
          }

          (outputPath ** "*.java").get
        }.toSet
      }
      cachedCompile(targets.map(_.headerFile).toSet).toSeq
    }
  }

}


