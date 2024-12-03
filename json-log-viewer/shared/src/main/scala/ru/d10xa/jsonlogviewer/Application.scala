package ru.d10xa.jsonlogviewer

import cats.data.Validated
import cats.effect.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.*
import fs2.io.*
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser
import _root_.io.circe.yaml.scalayaml.parser
import cats.syntax.all.*
import ru.d10xa.jsonlogviewer.decline.ConfigInitImpl
import ru.d10xa.jsonlogviewer.decline.ConfigYaml

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):

  private val stdinLinesStream: Stream[IO, String] =
    stdinUtf8[IO](1024 * 1024 * 10)
      .repartition(s => Chunk.array(s.split("\n", -1)))
      .filter(_.nonEmpty)

  import cats.effect.unsafe.implicits._

  def main: Opts[IO[ExitCode]] = DeclineOpts.config.map { c =>
    new ConfigInitImpl().initConfig(c).flatMap { updatedConfig =>
      IO {
        val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
        val logLineParser = updatedConfig.formatIn match {
          case Some(FormatIn.Logfmt) => LogfmtLogLineParser(updatedConfig)
          case _ => JsonLogLineParser(updatedConfig, jsonPrefixPostfix)
        }

        stdinLinesStream
          .through(LogViewerStream.stream[IO](updatedConfig, logLineParser))
          .through(text.utf8.encode)
          .through(io.stdout)
          .compile
          .drain
          .as(ExitCode.Success)
      }.flatten
    }
  }
