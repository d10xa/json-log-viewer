package ru.d10xa.jsonlogviewer.shell

import cats.effect.*
import fs2.*

import java.io.*

class ShellImpl[F[_]] extends Shell[F] {

  def mergeCommands(commands: List[String]): Stream[F, String] = Stream.empty

}
