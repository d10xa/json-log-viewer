package ru.d10xa.jsonlogviewer.shell
import fs2.*
import cats.effect.*

trait Shell[F[_]] {
  def mergeCommands(commands: List[String]): Stream[F, String]
}
