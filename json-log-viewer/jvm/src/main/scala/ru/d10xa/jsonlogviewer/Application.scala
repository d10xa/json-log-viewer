package ru.d10xa.jsonlogviewer

import cats.effect.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.*
import ru.d10xa.jsonlogviewer.decline.ConfigInit
import ru.d10xa.jsonlogviewer.decline.ConfigInitImpl
import ru.d10xa.jsonlogviewer.decline.DeclineOpts

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):

  private val configInit: ConfigInit = new ConfigInitImpl

  def main: Opts[IO[ExitCode]] = DeclineOpts.config.map { config =>
    configInit.initConfigYaml(config).use { configRef =>
      LogViewerStream
        .stream(config, configRef)
        .through(text.utf8.encode)
        .through(fs2.io.stdout)
        .compile
        .drain
        .as(ExitCode.Success)
    }

  }
