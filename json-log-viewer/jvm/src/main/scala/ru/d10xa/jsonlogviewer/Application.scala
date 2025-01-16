package ru.d10xa.jsonlogviewer

import cats.effect.*
import cats.effect.std.Supervisor
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
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
    Supervisor[IO].use { supervisor =>
      configInit.initConfigYaml(config, supervisor).use { configRef =>
        LogViewerStream
          .stream(config, configRef)
          .through(text.utf8.encode)
          .through(fs2.io.stdout)
          .compile
          .drain
          .as(ExitCode.Success)
      }
    }
    

  }
