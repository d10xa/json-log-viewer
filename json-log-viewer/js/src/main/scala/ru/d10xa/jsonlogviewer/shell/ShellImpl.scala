package ru.d10xa.jsonlogviewer.shell

import fs2.*

class ShellImpl[F[_]] extends Shell[F] {

  def mergeCommands(commands: List[String]): Stream[F, String] = Stream.empty

}
