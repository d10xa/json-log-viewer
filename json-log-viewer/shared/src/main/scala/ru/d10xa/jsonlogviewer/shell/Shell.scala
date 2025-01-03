package ru.d10xa.jsonlogviewer.shell
import cats.effect.IO
import fs2.*

trait Shell {
  def mergeCommandsAndInlineInput(commands: List[String], inlineInput: Option[String]): Stream[IO, String]
}
