package ru.d10xa.jsonlogviewer

import _root_.io.circe.*
import _root_.io.circe.parser.*
import cats.effect.*
import cats.effect.kernel.Sync
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import com.monovore.decline.time.*
import fs2.*
import fs2.io.*

import java.time.ZonedDateTime

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):
  val timestampAfter: Opts[Option[ZonedDateTime]] =
    Opts.option[ZonedDateTime]("timestamp-after", "").orNone
  val timestampBefore: Opts[Option[ZonedDateTime]] =
    Opts.option[ZonedDateTime]("timestamp-before", "").orNone
  val timestampField: Opts[String] =
    Opts
      .option[String]("timestamp-field", help = "")
      .withDefault("@timestamp")
  val stringStream: Stream[IO, String] = stdinUtf8[IO](1024 * 1024 * 10)
    .repartition(s => Chunk.array(s.split("\n", -1)))
    .filter(_.nonEmpty)
  def timestampConfig: Opts[TimestampConfig] =
    (timestampField, timestampAfter, timestampBefore)
      .mapN(TimestampConfig.apply)
  val config: Opts[Config] = timestampConfig.map(Config.apply)

  def main: Opts[IO[ExitCode]] = config.map { c =>
    val timestampFilter = TimestampFilter()
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    val logLineParser = LogLineParser(c, jsonPrefixPostfix)
    val outputLineFormatter = ColorLineFormatter(c)
    val stream: Stream[IO, Nothing] =
      stringStream
        .map(logLineParser.parse)
        .through(timestampFilter.filterTimestampAfter[IO](c.timestamp.after))
        .through(timestampFilter.filterTimestampBefore[IO](c.timestamp.before))
        .map(outputLineFormatter.formatLine)
        .map(_.toString)
        .intersperse("\n")
        .append(Stream.emit("\n"))
        .through(text.utf8.encode)
        .through(io.stdout)
    stream.compile.drain.as(ExitCode.Success)
  }
