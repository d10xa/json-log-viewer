package ru.d10xa.jsonlogviewer.shell

import cats.effect.IO
import fs2.*

class ShellImpl extends Shell {

  def mergeCommandsAndInlineInput(
    commands: List[String],
    inlineInput: Option[String]
  ): Stream[IO, String] = inlineInput match
    case Some(inlineInput) =>
      Shell.stringToStream(inlineInput)
    case None => Stream.empty

  def mergeCommandsAndInlineInputWithRestart(
    commands: List[String],
    inlineInput: Option[String],
    restartConfig: RestartConfig,
    onRestart: IO[Unit]
  ): Stream[IO, String] =
    // JS doesn't execute shell commands, only supports inline input
    mergeCommandsAndInlineInput(commands, inlineInput)

}
