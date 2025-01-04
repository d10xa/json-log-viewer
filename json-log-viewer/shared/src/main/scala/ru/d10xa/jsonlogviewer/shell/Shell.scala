package ru.d10xa.jsonlogviewer.shell
import cats.effect.IO
import fs2.*

trait Shell {
  def mergeCommandsAndInlineInput(commands: List[String], inlineInput: Option[String]): Stream[IO, String]
}

object Shell:
  def stringToStream(str: String): fs2.Stream[IO, String] =
    val strings = str.split("\n").filter(_.nonEmpty)
    fs2.Stream.emits(strings)
end Shell
