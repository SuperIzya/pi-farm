// from com.timcharper.sbt

import java.io.File

import sbt._, Keys._

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
    val jnaeratorEngine = settingKey[ModuleID]("the engine used")
  }

  import autoImport._

  override lazy val globalSettings = Seq(
    jnaVersion := "4.2.1",
    bridjVersion := "0.7.0",
    jnaeratorRuntime := Runtime.BridJ,
  )

  lazy val jnaeratorSettings: Seq[Setting[_]] = Seq(
    sourceDirectory := (sourceDirectory in Compile) {
      _ / "native"
    }.value,
    sourceDirectories := (sourceDirectory in Compile) {
      _ :: Nil
    }.value,
    sourceManaged := (sourceManaged in Compile) {
      _ / "jnaerator_interfaces"
    }.value,
    jnaeratorTargets := Nil,

    jnaeratorEngine := {
      (Jnaerator / jnaeratorRuntime).value match {
        case Runtime.JNA => "net.java.dev.jna" % "jna" % (Jnaerator / jnaVersion).value
        case Runtime.BridJ => "com.nativelibs4java" % "bridj" % (Jnaerator / bridjVersion).value
      }
    },

    cleanFiles += (Jnaerator / sourceManaged).value,

    // watchSources ++= (jnaeratorTargets in JnaeratorConfig).flatMap(_.join).map { _.map(_.headerFile) }.value,
    watchSources ++= (Jnaerator / jnaeratorTargets).map {
      _.map(_.headerFile)
    }.value,
    watchSources += file("."),

    Compile / managedSourceDirectories += (Jnaerator / sourceManaged).value,

    jnaeratorGenerate := JnaeratorCmd(
      (Jnaerator / jnaeratorTargets).value,
      streams.value,
      (Jnaerator / jnaeratorRuntime).value,
      (Jnaerator / sourceManaged).value
    ),

    Compile / sourceGenerators += (Jnaerator / jnaeratorGenerate).taskValue,
  )

  override def projectSettings: Seq[Def.Setting[_]] = inConfig(Jnaerator)(jnaeratorSettings)


  private object JnaeratorCmd {
    def apply(targets: Seq[JnaeratorTarget],
                 streams: TaskStreams,
                 runtime: Runtime,
                 outputPath: File
                ): Seq[File] = {

      val targetId = "c" + targets.toList.map { target =>
        (target, runtime, outputPath)
      }.hashCode
      val cachedCompile = FileFunction.cached(streams.cacheDirectory / "jnaerator" / targetId, inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { _: Set[File] =>
        IO.delete(outputPath)
        outputPath.mkdirs()

        targets.flatMap { target =>
          // java -jar bin/jnaerator.jar -package com.package.name -library libName lib/libName.h -o src/main/java -mode Directory -f -scalaStructSetters
          val args = List(
            "-package", target.packageName,
            "-library", target.libraryName,
            "-o", outputPath.getCanonicalPath,
            "-mode", "Directory",
            "-f",
            "-scalaStructSetters"
          ) ++ target.extraArgs ++ Seq(target.headerFile.getCanonicalPath)

          streams.log.info(s"(${target.headerFile.getName}) Running JNAerator with args ${args.mkString(" ")}")
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


