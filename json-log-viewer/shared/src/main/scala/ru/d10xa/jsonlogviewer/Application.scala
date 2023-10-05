package ru.d10xa.jsonlogviewer

import _root_.io.circe.*
import _root_.io.circe.parser.*
import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.kernel.Sync
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import com.monovore.decline.time.*
import fs2.*
import fs2.io.*
import cats.data.Validated
import cats.data.ValidatedNel

import java.time.ZonedDateTime
import scala.util.matching.Regex

object Application
  extends CommandIOApp(
    "json-log-viewer",
    "Print json logs in human-readable form"
  ):
  case class ConfigGrep(key: String, value: Regex)
  val timestampAfter: Opts[Option[ZonedDateTime]] =
    Opts.option[ZonedDateTime]("timestamp-after", "").orNone
  val timestampBefore: Opts[Option[ZonedDateTime]] =
    Opts.option[ZonedDateTime]("timestamp-before", "").orNone
  val timestampField: Opts[String] =
    Opts
      .option[String]("timestamp-field", help = "")
      .withDefault("@timestamp")

  def validateConfigGrep(string: String): ValidatedNel[String, ConfigGrep] =
    string.split(":", 2) match {
      case Array(key, value) =>
        Validated.valid(ConfigGrep(key, value.r))
      case _ => Validated.invalidNel(s"Invalid key:value pair: $string")
    }

  val grepConfig: Opts[List[ConfigGrep]] = Opts
    .options[String]("grep", "", metavar = "key:value")
    .mapValidated { lines => lines.traverse(validateConfigGrep) }
    .orEmpty

  val stringStream: Stream[IO, String] = stdinUtf8[IO](1024 * 1024 * 10)
    .repartition(s => Chunk.array(s.split("\n", -1)))
    .filter(_.nonEmpty)
  def timestampConfig: Opts[TimestampConfig] =
    (timestampField, timestampAfter, timestampBefore)
      .mapN(TimestampConfig.apply)
  val config: Opts[Config] = (timestampConfig, grepConfig)
    .mapN(Config.apply)

  def main: Opts[IO[ExitCode]] = config.map { c =>
    val timestampFilter = TimestampFilter()
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    val logLineParser = LogLineParser(c, jsonPrefixPostfix)
    val outputLineFormatter = ColorLineFormatter(c)
    val logLineFilter = LogLineFilter(c)
    val stream: Stream[IO, Nothing] =
      stringStream
        .map(logLineParser.parse)
        .filter(logLineFilter.grep)
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
