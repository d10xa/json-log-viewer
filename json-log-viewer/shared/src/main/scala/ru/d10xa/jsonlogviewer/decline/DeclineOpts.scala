package ru.d10xa.jsonlogviewer.decline

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.Opts
import com.monovore.decline.time.*
import ru.d10xa.jsonlogviewer.decline.Config.ConfigGrep
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut
import ru.d10xa.jsonlogviewer.query.QueryAST
import ru.d10xa.jsonlogviewer.query.QueryCompiler

import java.time.ZonedDateTime

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
    .mapValidated(QueryASTValidator.toValidatedQueryAST)
    .orNone

  val formatIn: Opts[Option[FormatIn]] = Opts
    .option[String]("format-in", help = "json, logfmt")
    .mapValidated(FormatInValidator.toValidatedFormatIn)
    .orNone

  val formatOut: Opts[Option[FormatOut]] = Opts
    .option[String]("format-out", help = "pretty, raw")
    .mapValidated(FormatOutValidator.toValidatedFormatOut)
    .orNone

  def timestampConfig: Opts[TimestampConfig] =
    (timestampField, timestampAfter, timestampBefore)
      .mapN(TimestampConfig.apply)

  val configFile: Opts[Option[ConfigFile]] = Opts
    .option[String]("config-file", help = "Path to configuration file")
    .map(ConfigFile.apply)
    .orNone

  val config: Opts[Config] =
    (configFile, timestampConfig, grepConfig, filterConfig, formatIn, formatOut)
      .mapN {
        case (
            configFile,
            timestampConfig,
            grepConfig,
            filterConfig,
            formatIn,
            formatOut
          ) =>
          Config(
            configFile = configFile,
            configYaml = None,
            timestamp = timestampConfig,
            grep = grepConfig,
            filter = filterConfig,
            formatIn = formatIn,
            formatOut = formatOut
          )
      }

  val command: Command[Config] = Command(
    name = "json-log-viewer",
    header = "Print json logs in human-readable form",
    helpFlag = true
  ).apply(config)
}
