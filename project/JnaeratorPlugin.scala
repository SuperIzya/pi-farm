// from com.timcharper.sbt

import java.io.File
import sbt.{config => sbtConfig, _}
import sbt.Keys.{cleanFiles, libraryDependencies, managedSourceDirectories,
  sourceDirectories, sourceDirectory, sourceGenerators, sourceManaged, streams,
  version, watchSources}

object JnaeratorPlugin extends AutoPlugin {
  object autoImport {

    val JnaeratorConfig = sbtConfig("jnaerator")

    val jnaeratorTargets = TaskKey[Seq[Jnaerator.Target]]("jnaerator-targets",
      "List of header-files and corresponding configuration for java interface generation")
    val jnaeratorGenerate = TaskKey[Seq[File]]("jnaerator-generate",
      "Run jnaerate and generate interfaces")
    val jnaeratorRuntime = SettingKey[Jnaerator.Runtime]("which runtime to use")

    val jnaVersion = SettingKey[String]("jna version")
    val bridjVersion = SettingKey[String]("bridJ version")

    val jnaeratorEngine = SettingKey[ModuleID]("the engine used")

    object Jnaerator {
      sealed trait Runtime
      object Runtime {
        case object JNA extends Runtime
        case object BridJ extends Runtime
      }
      case class Target( headerFile: File,
                         packageName: String,
                         libraryName: String,
                         extraArgs: Seq[String] = Seq.empty)

      lazy val settings = inConfig(JnaeratorConfig)(Seq[Setting[_]](

        sourceDirectory := (sourceDirectory in Compile) { _ / "native" }.value,
        sourceDirectories := (sourceDirectory in Compile) { _ :: Nil }.value,
        sourceManaged := (sourceManaged in Compile) { _ / "jnaerator_interfaces" }.value,
        jnaeratorGenerate := runJnaerator.value

      )) ++ Seq[Setting[_]](

        jnaeratorTargets := Nil,
        jnaeratorRuntime := Runtime.BridJ,
        jnaeratorEngine := {
          (JnaeratorConfig / jnaeratorRuntime).value match {
            case Runtime.JNA => "net.java.dev.jna" % "jna" % (JnaeratorConfig / jnaVersion).value
            case Runtime.BridJ => "com.nativelibs4java" % "bridj" % (JnaeratorConfig / bridjVersion).value
          }
        },
        cleanFiles += (JnaeratorConfig / sourceManaged).value,

        // watchSources ++= (jnaeratorTargets in JnaeratorConfig).flatMap(_.join).map { _.map(_.headerFile) }.value,
        watchSources ++= (JnaeratorConfig / jnaeratorTargets).map { _.map(_.headerFile) }.value,
        watchSources += file("."),

        sourceGenerators in Compile += (JnaeratorConfig / jnaeratorGenerate).taskValue,
        managedSourceDirectories in Compile += (JnaeratorConfig / sourceManaged).value,
        libraryDependencies += jnaeratorEngine.value
      )
    }

    private def runJnaerator: Def.Initialize[Task[Seq[File]]] = Def.task {

      val targets = (JnaeratorConfig / jnaeratorTargets).value
      val s = streams.value
      val runtime = (JnaeratorConfig / jnaeratorRuntime).value
      val outputPath = (JnaeratorConfig / sourceManaged).value

      val targetId = "c" + targets.toList.map { target =>
        (target, runtime, outputPath)
      }.hashCode
      val cachedCompile = FileFunction.cached(s.cacheDirectory / "jnaerator" / targetId, inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { _: Set[File] =>
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

          s.log.info(s"(${target.headerFile.getName}) Running JNAerator with args ${args.mkString(" ")}")
          try {
            com.ochafik.lang.jnaerator.JNAerator.main(args.toArray)
          } catch { case e: Exception =>
            throw new RuntimeException(s"error occured while running jnaerator: ${e.getMessage}", e)
          }

          (outputPath ** "*.java").get
        }.toSet
      }
      cachedCompile(targets.map(_.headerFile).toSet).toSeq
    }
  }


}