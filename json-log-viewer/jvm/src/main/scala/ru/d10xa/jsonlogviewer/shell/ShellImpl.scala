package ru.d10xa.jsonlogviewer.shell

import cats.effect.*
import fs2.*

import java.io.*

class ShellImpl extends Shell {

  def createProcess(command: String): Resource[IO, Process] =
    Resource.make(IO {
      new ProcessBuilder("sh", "-c", command)
        .redirectErrorStream(true)
        .start()
    })(process => IO(process.destroy()).void)

  def runInfiniteCommand(command: String): Stream[IO, String] =
    Stream.resource(createProcess(command)).flatMap { process =>
      fs2.io
        .readInputStream(
          IO(process.getInputStream),
          4096,
          closeAfterUse = false
        )
        .through(text.utf8.decode)
        .through(text.lines)
        .onFinalize(IO {
          process.waitFor()
        }.void)
    }

  def mergeCommands(commands: List[String]): Stream[IO, String] = {
    val streams = commands.map(runInfiniteCommand)
    Stream.emits(streams).parJoin(math.max(1, commands.length))
  }

}
