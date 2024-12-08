package ru.d10xa.jsonlogviewer.shell

import cats.effect.*
import fs2.*

import java.io.*

class ShellImpl extends Shell {

  def mergeCommands(commands: List[String]): Stream[IO, String] = Stream.empty

}
