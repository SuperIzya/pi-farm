package org.pi.farm.utils

import zio.{Chunk, Scope, Task, ZIO}
import zio.compress.{ArchiveEntry, GzipCompressor, GzipDecompressor, TarArchiver, TarUnarchiver}
import zio.stream.{ZSink, ZStream}

import java.nio.file.{Files, Path}

object Archive {
  type EntryStream[+Size[A] <: Option[A]]       = ZStream[Any, Throwable, Entry[Size]]
  type StatefullEntry[+Size[A] <: Option[A], S] = Task[(S, Chunk[Entry[Size]])]
  type Entry[+Size[A] <: Option[A]]             = (ArchiveEntry[Size, Any], ContentStream)
  type ImportEntry                              = (String, Path)
  type ContentStream                            = ZStream[Any, Throwable, Byte]

  extension [Size[A] <: Option[A], S](entry: StatefullEntry[Size, S]) {
    def toStream: EntryStream[Size] =
      ZStream.fromZIO(entry).map(_._2).flattenChunks
  }

  def archiveEntry(name: String, content: Array[Byte]): Entry[Some] =
    (ArchiveEntry(name, Some(content.length.toLong)), ZStream.fromIterable(content))

  def archiveEntry(name: String, content: Task[(Int, ContentStream)]): Task[Entry[Some]] =
    for {
      (size, stream) <- content
    } yield (ArchiveEntry(name, Some(size.toLong)), stream)

  extension (stream: EntryStream[Some]) {
    def compress: ContentStream =
      stream.via(TarArchiver.archive >>> GzipCompressor.compress)
  }

  extension (stream: ContentStream) {
    def decompress: ZIO[Scope, Throwable, Chunk[ImportEntry]] =
      for {
        directory <- ZIO.acquireRelease(
                       ZIO.attemptBlocking {
                         val path = Files.createTempDirectory(Path.of("/tmp"), "pi-farm-import-")
                         path.toFile.deleteOnExit()
                         path
                       }
                     )(directory =>
                       ZStream
                         .fromJavaStream(Files.walk(directory))
                         .runCollect
                         .flatMap(paths =>
                           ZIO.foreachDiscard(paths.sortBy(_.getNameCount).reverse)(path =>
                             ZIO.attemptBlocking(Files.delete(path))
                           )
                         )
                         .orDie
                     )
        archive    = directory.resolve("upload.tar.gz")
        contents   = directory.resolve("contents")
        _         <- stream.run(ZSink.fromPath(archive))
        entries   <- ZStream
                       .fromPath(archive)
                       .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
                       .mapZIO {
                         case (entry, content) =>
                           for {
                             relative <- ZIO.attempt(Path.of(entry.name))
                             target    = contents.resolve(relative).normalize()
                             _        <-
                               ZIO
                                 .fail(
                                   new IllegalArgumentException(
                                     s"Archive entry outside import directory: ${entry.name}"
                                   )
                                 )
                                 .when(
                                   relative.isAbsolute ||
                                     !target.startsWith(contents) ||
                                     target == contents
                                 )
                             path     <- ZIO.attemptBlocking {
                                           Files.createDirectories(target.getParent)
                                           Files.createFile(target)
                                         }
                             _        <- content.run(ZSink.fromPath(path))
                           } yield (entry.name, path)
                       }
                       .runCollect
      } yield entries
  }

}
