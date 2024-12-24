package ru.d10xa.jsonlogviewer
import cats.effect.IO

trait StdInLinesStream {
  def stdinLinesStream: fs2.Stream[IO, String]
}
