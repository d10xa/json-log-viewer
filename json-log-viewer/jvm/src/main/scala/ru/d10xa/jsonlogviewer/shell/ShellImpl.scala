package ru.d10xa.jsonlogviewer.shell

import cats.effect.*
import cats.syntax.all.*

class ShellImpl extends Shell {

  def createProcess(command: String): Resource[IO, Process] =
    Resource.make(IO {
      new ProcessBuilder("sh", "-c", command)
        .redirectErrorStream(true)
        .start()
    })(process => IO(process.destroy()))

  def runInfiniteCommand(command: String): fs2.Stream[IO, String] =
    fs2.Stream.resource(createProcess(command)).flatMap { process =>
      fs2.io
        .readInputStream(
          IO(process.getInputStream),
          4096,
          closeAfterUse = false
        )
        .through(fs2.text.utf8.decode)
        .through(fs2.text.lines)
        .onFinalize(IO{
          process.waitFor()
        }.void)
    }

  def mergeCommands(commands: List[String]): fs2.Stream[IO, String] = {
    val streams = commands.map(runInfiniteCommand)
    fs2.Stream.emits(streams).parJoin(math.max(1, commands.length))
  }

}