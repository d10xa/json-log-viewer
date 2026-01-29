package ru.d10xa.jsonlogviewer.shell

import cats.effect.*
import cats.syntax.all.*
import scala.concurrent.duration.*

class ShellImpl extends Shell {

  def createProcess(command: String): Resource[IO, Process] =
    Resource.make(IO {
      new ProcessBuilder("sh", "-c", command)
        .redirectErrorStream(true)
        .start()
    })(process => IO(process.destroy()))

  private def runInfiniteCommand(command: String): fs2.Stream[IO, String] =
    fs2.Stream.resource(createProcess(command)).flatMap { process =>
      fs2.io
        .readInputStream(
          IO(process.getInputStream),
          4096,
          closeAfterUse = false
        )
        .through(fs2.text.utf8.decode)
        .through(fs2.text.lines)
        .onFinalize(IO {
          process.waitFor()
        }.void)
    }

  private def runCommandWithRestart(
    command: String,
    restartConfig: RestartConfig,
    onRestart: IO[Unit]
  ): fs2.Stream[IO, String] = {
    def loop(restartCount: Int): fs2.Stream[IO, String] = {
      val canRestart = restartConfig.maxRestarts.forall(restartCount < _)

      runInfiniteCommand(command) ++ (
        if (restartConfig.enabled && canRestart)
          fs2.Stream.eval(onRestart) >>
            fs2.Stream.sleep[IO](restartConfig.delayMs.millis) >>
            loop(restartCount + 1)
        else
          fs2.Stream.empty
      )
    }

    if (restartConfig.enabled) loop(0) else runInfiniteCommand(command)
  }

  def mergeCommandsAndInlineInput(
    commands: List[String],
    inlineInput: Option[String]
  ): fs2.Stream[IO, String] = {
    val streams =
      commands
        .map(runInfiniteCommand) ++ inlineInput.map(Shell.stringToStream).toList
    fs2.Stream.emits(streams).parJoin(math.max(1, commands.length))
  }

  def mergeCommandsAndInlineInputWithRestart(
    commands: List[String],
    inlineInput: Option[String],
    restartConfig: RestartConfig,
    onRestart: IO[Unit]
  ): fs2.Stream[IO, String] = {
    val commandStreams =
      commands.map(cmd => runCommandWithRestart(cmd, restartConfig, onRestart))
    val inlineStream = inlineInput.map(Shell.stringToStream).toList
    val streams = commandStreams ++ inlineStream
    fs2.Stream.emits(streams).parJoin(math.max(1, streams.length))
  }

}
