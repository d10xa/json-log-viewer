package ru.d10xa.jsonlogviewer.shell

import cats.effect.*
import cats.syntax.all.*

class ShellImpl[F[_]: Async] extends Shell[F] {

  def createProcess(command: String): Resource[F, Process] =
    Resource.make(Async[F].delay {
      new ProcessBuilder("sh", "-c", command)
        .redirectErrorStream(true)
        .start()
    })(process => Async[F].delay(process.destroy()))

  def runInfiniteCommand(command: String): fs2.Stream[F, String] =
    fs2.Stream.resource(createProcess(command)).flatMap { process =>
      fs2.io
        .readInputStream(
          Async[F].delay(process.getInputStream),
          4096,
          closeAfterUse = false
        )
        .through(fs2.text.utf8.decode)
        .through(fs2.text.lines)
        .onFinalize(Async[F].delay {
          process.waitFor()
        }.void)
    }

  def mergeCommands(commands: List[String]): fs2.Stream[F, String] = {
    val streams = commands.map(runInfiniteCommand)
    fs2.Stream.emits(streams).parJoin(math.max(1, commands.length))
  }

}