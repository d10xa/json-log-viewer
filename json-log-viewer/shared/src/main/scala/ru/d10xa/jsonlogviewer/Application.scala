package ru.d10xa.jsonlogviewer

import cats.effect.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.*
import fs2.io.*
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.ConfigInit
import ru.d10xa.jsonlogviewer.decline.ConfigInitImpl
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser
import ru.d10xa.jsonlogviewer.shell.ShellImpl

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):

  private val configInit: ConfigInit = new ConfigInitImpl

  def main: Opts[IO[ExitCode]] = DeclineOpts.config.map { c =>
    configInit.initConfig(c).flatMap { updatedConfig =>
      IO {
        LogViewerStream
          .stream[IO](updatedConfig)
          .through(text.utf8.encode)
          .through(io.stdout)
          .compile
          .drain
          .as(ExitCode.Success)
      }.flatten
    }
  }
