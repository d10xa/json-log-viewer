package ru.d10xa.jsonlogviewer.shell

import cats.effect.IO
import fs2.*

class ShellImpl extends Shell {

  def mergeCommandsAndInlineInput(
    commands: List[String],
    inlineInput: Option[String]
  ): Stream[IO, String] = inlineInput match
    case Some(inlineInput) =>
      println(s"mergeCommandsAndInlineInput.inlineInput = ${inlineInput.length}")
      Stream.eval(IO(inlineInput.trim))
    case None              => Stream.empty

}
