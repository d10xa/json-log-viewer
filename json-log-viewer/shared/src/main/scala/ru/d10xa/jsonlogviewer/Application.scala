package ru.d10xa.jsonlogviewer

import _root_.io.circe.*
import _root_.io.circe.parser.*
import cats.effect.*
import cats.effect.kernel.Sync
import cats.syntax.all.*
import fs2.*
import fs2.io.*

import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

import java.time.ZonedDateTime
import scala.util.matching.Regex

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):

  private val stdinLinesStream: Stream[IO, String] = stdinUtf8[IO](1024 * 1024 * 10)
    .repartition(s => Chunk.array(s.split("\n", -1)))
    .filter(_.nonEmpty)

  def jsonLogViewerStream[F[_]](
    config: Config
  ): Pipe[F, String, String] = stream =>
    val timestampFilter = TimestampFilter()
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    val logLineParser = LogLineParser(config, jsonPrefixPostfix)
    val outputLineFormatter = ColorLineFormatter(config)
    val logLineFilter = LogLineFilter(config)
    stream
      .map(logLineParser.parse)
      .filter(logLineFilter.grep)
      .through(timestampFilter.filterTimestampAfter[F](config.timestamp.after))
      .through(
        timestampFilter.filterTimestampBefore[F](config.timestamp.before)
      )
      .map(outputLineFormatter.formatLine)
      .map(_.toString)
      .intersperse("\n")
      .append(Stream.emit("\n"))

  def main: Opts[IO[ExitCode]] = DeclineOpts.config.map { c =>
      stdinLinesStream
        .through(jsonLogViewerStream[IO](c))
        .through(text.utf8.encode)
        .through(io.stdout)
        .compile.drain.as(ExitCode.Success)
  }
