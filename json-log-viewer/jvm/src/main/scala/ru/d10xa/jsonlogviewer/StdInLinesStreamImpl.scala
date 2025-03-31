package ru.d10xa.jsonlogviewer
import cats.effect.IO
import fs2.io.stdinUtf8
import fs2.Chunk
import fs2.Stream

class StdInLinesStreamImpl extends StdInLinesStream {

  override def stdinLinesStream: Stream[IO, String] =
    stdinUtf8[IO](1024 * 1024 * 10)
      .repartition(s => Chunk.array(s.split("\n", -1)))
      .filter(_.nonEmpty)
}
