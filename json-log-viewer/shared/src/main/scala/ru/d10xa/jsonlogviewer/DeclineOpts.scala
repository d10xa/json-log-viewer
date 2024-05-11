package ru.d10xa.jsonlogviewer

import com.monovore.decline.Opts
import com.monovore.decline.*
import com.monovore.decline.effect.*
import com.monovore.decline.time.*

import java.time.ZonedDateTime
import cats.data.Validated
import cats.data.ValidatedNel
import cats.data.NonEmptyList
import cats.syntax.all.*
import ru.d10xa.jsonlogviewer.Config.ConfigGrep
import ru.d10xa.jsonlogviewer.Config.FormatIn
import ru.d10xa.jsonlogviewer.query.QueryAST
import ru.d10xa.jsonlogviewer.query.QueryCompiler

object DeclineOpts {

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

  val filterConfig: Opts[Option[QueryAST]] = Opts
    .option[String]("filter", "sql expression")
    .mapValidated { str =>
      QueryCompiler(str) match
        case Left(value) => Validated.invalidNel(value.toString)
        case Right(value) => Validated.validNel(value)
    }.orNone

  val formatIn: Opts[FormatIn] = Opts.option[String]("format-in", help = "json, logfmt")
    .mapValidated {
      case "json" => Validated.valid(FormatIn.Json)
      case "logfmt" => Validated.valid(FormatIn.Logfmt)
      case other => Validated.invalidNel(s"Wrong format: $other")
    }.withDefault(FormatIn.Json)

  def timestampConfig: Opts[TimestampConfig] =
    (timestampField, timestampAfter, timestampBefore)
      .mapN(TimestampConfig.apply)

  val config: Opts[Config] = (timestampConfig, grepConfig, filterConfig, formatIn)
    .mapN(Config.apply)
  
  val command: Command[Config] = Command(
    name = "json-log-viewer",
    header = "Print json logs in human-readable form",
    helpFlag = true
  ).apply(config)
}
