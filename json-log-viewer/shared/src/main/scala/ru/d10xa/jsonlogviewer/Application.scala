package ru.d10xa.jsonlogviewer

import cats.effect.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.*
import fs2.io.*
import ru.d10xa.jsonlogviewer.Config.FormatIn
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):

  private val stdinLinesStream: Stream[IO, String] =
    stdinUtf8[IO](1024 * 1024 * 10)
      .repartition(s => Chunk.array(s.split("\n", -1)))
      .filter(_.nonEmpty)

  def main: Opts[IO[ExitCode]] = DeclineOpts.config.map { c =>
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    val logLineParser = c.formatIn match
      case FormatIn.Json => JsonLogLineParser(c, jsonPrefixPostfix)
      case FormatIn.Logfmt => LogfmtLogLineParser(c)

    stdinLinesStream
      .through(LogViewerStream.stream[IO](c, logLineParser))
      .through(text.utf8.encode)
      .through(io.stdout)
      .compile
      .drain
      .as(ExitCode.Success)
  }
