package ru.d10xa.jsonlogviewer.shell
import fs2.*

trait Shell[F[_]] {
  def mergeCommands(commands: List[String]): Stream[F, String]
}
