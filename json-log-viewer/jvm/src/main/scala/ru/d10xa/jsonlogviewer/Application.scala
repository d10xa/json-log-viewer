package ru.d10xa.jsonlogviewer

import cats.effect.*
import cats.effect.std.Supervisor
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import fs2.*
import ru.d10xa.jsonlogviewer.decline.ConfigInit
import ru.d10xa.jsonlogviewer.decline.ConfigInitImpl
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.shell.ShellImpl

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):

  private val configInit: ConfigInit = new ConfigInitImpl

  def main: Opts[IO[ExitCode]] = DeclineOpts.config.map { config =>
    Supervisor[IO].use { supervisor =>
      configInit.initConfigRefs(config, supervisor).use { configRefs =>
        val ctx = StreamContext(
          config = config,
          configYamlRef = configRefs.configYamlRef,
          cacheRef = configRefs.cacheRef,
          stdinStream = new StdInLinesStreamImpl,
          shell = new ShellImpl
        )
        LogViewerStream.stream(ctx)
          .through(text.utf8.encode)
          .through(fs2.io.stdout)
          .compile
          .drain
          .as(ExitCode.Success)
      }
    }

  }
