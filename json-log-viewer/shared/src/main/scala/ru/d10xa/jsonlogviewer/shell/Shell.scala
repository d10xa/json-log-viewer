package ru.d10xa.jsonlogviewer.shell
import fs2.*
import cats.effect.*

trait Shell {
  def mergeCommands(commands: List[String]): Stream[IO, String]
}
