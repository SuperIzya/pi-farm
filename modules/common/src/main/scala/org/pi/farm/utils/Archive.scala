package org.pi.farm.utils

import zio.{Chunk, Scope, Task, ZIO}
import zio.compress.{ArchiveEntry, GzipCompressor, GzipDecompressor, TarArchiver, TarUnarchiver}
import zio.stream.{ZSink, ZStream}

object Archive {
  type EntryStream[+Size[A] <: Option[A]]       = ZStream[Any, Throwable, Entry[Size]]
  type StatefullEntry[+Size[A] <: Option[A], S] = Task[(S, Chunk[Entry[Size]])]
  type Entry[+Size[A] <: Option[A]]             = (ArchiveEntry[Size, Any], ContentStream)
  type ImportSubEntry                           = (String, os.SubPath)
  type ImportEntry                              = (String, os.Path)
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
        directory <-
          ZIO.acquireRelease(
            ZIO.attemptBlocking {
              os.temp.dir(prefix = "pi-farm-import-", dir = os.Path("/tmp"), deleteOnExit = true)
            }
          )(directory => ZIO.attemptBlocking(os.remove.all(directory)).orDie)
        archive    = directory / "upload.tar.gz"
        contents   = directory / "contents"
        _         <- stream.run(ZSink.fromPath(archive.toNIO))
        entries   <- ZStream
                       .fromPath(archive.toNIO)
                       .via(GzipDecompressor.decompress >>> TarUnarchiver.unarchive)
                       .mapZIO {
                         case (entry, content) =>
                           for {
                             relative <- ZIO.attempt(os.SubPath(entry.name))
                             target    = contents / relative
                             _        <- ZIO.attemptBlocking {
                                           os.remove.all(target, true)
                                           os.makeDir.all(target / os.up)
                                           os.write(target, Array.emptyByteArray)
                                         }
                             _        <- content.run(ZSink.fromPath(target.toNIO))
                           } yield (entry.name, target)
                       }
                       .runCollect
      } yield entries
  }

}
