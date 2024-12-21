package ru.d10xa.jsonlogviewer.shell

import cats.effect.IO
import fs2.*

class ShellImpl extends Shell {

  def mergeCommands(commands: List[String]): Stream[IO, String] = Stream.empty

}
