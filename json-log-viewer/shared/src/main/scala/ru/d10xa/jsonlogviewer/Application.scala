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

  def main: Opts[IO[ExitCode]] = DeclineOpts.config.map { c =>
      stdinLinesStream
        .through(JsonLogViewerStream.stream[IO](c))
        .through(text.utf8.encode)
        .through(io.stdout)
        .compile.drain.as(ExitCode.Success)
  }
