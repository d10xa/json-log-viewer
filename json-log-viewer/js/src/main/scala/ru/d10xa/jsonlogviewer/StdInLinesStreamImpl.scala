package ru.d10xa.jsonlogviewer
import cats.effect.IO

class StdInLinesStreamImpl extends StdInLinesStream {

  override def stdinLinesStream: fs2.Stream[IO, String] = fs2.Stream.empty
}
