package ru.d10xa.jsonlogviewer.decline

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.time.*
import com.monovore.decline.Opts
import java.time.ZonedDateTime
import ru.d10xa.jsonlogviewer.decline.Config.ConfigGrep
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut
import ru.d10xa.jsonlogviewer.query.QueryAST

object DeclineOpts {

  // Timestamp filter options
  val timestampAfter: Opts[Option[ZonedDateTime]] =
    Opts.option[ZonedDateTime]("timestamp-after", "").orNone
  val timestampBefore: Opts[Option[ZonedDateTime]] =
    Opts.option[ZonedDateTime]("timestamp-before", "").orNone

  // Field name options
  val timestampField: Opts[String] =
    Opts
      .option[String](
        "timestamp-field",
        help = "Override default timestamp field name"
      )
      .withDefault("@timestamp")

  val levelField: Opts[String] =
    Opts
      .option[String]("level-field", help = "Override default level field name")
      .withDefault("level")

  val messageField: Opts[String] =
    Opts
      .option[String](
        "message-field",
        help = "Override default message field name"
      )
      .withDefault("message")

  val stackTraceField: Opts[String] =
    Opts
      .option[String](
        "stack-trace-field",
        help = "Override default stack trace field name"
      )
      .withDefault("stack_trace")

  val loggerNameField: Opts[String] =
    Opts
      .option[String](
        "logger-name-field",
        help = "Override default logger name field name"
      )
      .withDefault("logger_name")

  val threadNameField: Opts[String] =
    Opts
      .option[String](
        "thread-name-field",
        help = "Override default thread name field name"
      )
      .withDefault("thread_name")

  def fieldNamesConfig: Opts[FieldNamesConfig] =
    (
      timestampField,
      levelField,
      messageField,
      stackTraceField,
      loggerNameField,
      threadNameField
    ).mapN(FieldNamesConfig.apply)

  def validateConfigGrep(string: String): ValidatedNel[String, ConfigGrep] =
    string.split(":", 2) match {
      case Array(key, value) =>
        Validated.valid(ConfigGrep(key, value.r))
      case _ => Validated.invalidNel(s"Invalid key:value pair: $string")
    }

  val grepConfig: Opts[List[ConfigGrep]] = Opts
    .options[String]("grep", "", metavar = "key:value")
    .mapValidated(lines => lines.traverse(validateConfigGrep))
    .orEmpty

  val filterConfig: Opts[Option[QueryAST]] = Opts
    .option[String]("filter", "sql expression")
    .mapValidated(QueryASTValidator.toValidatedQueryAST)
    .orNone

  val formatIn: Opts[Option[FormatIn]] = Opts
    .option[String]("format-in", help = "json, logfmt, csv")
    .mapValidated(FormatInValidator.toValidatedFormatIn)
    .orNone

  val formatOut: Opts[Option[FormatOut]] = Opts
    .option[String]("format-out", help = "pretty, raw")
    .mapValidated(FormatOutValidator.toValidatedFormatOut)
    .orNone

  def timestampConfig: Opts[TimestampConfig] =
    (timestampAfter, timestampBefore)
      .mapN(TimestampConfig.apply)

  val configFile: Opts[Option[ConfigFile]] = Opts
    .option[String]("config-file", help = "Path to configuration file")
    .map(ConfigFile.apply)
    .orNone

  val showEmptyFields: Opts[Boolean] = Opts
    .flag("show-empty-fields", help = "Show fields with empty values in output")
    .orFalse

  val config: Opts[Config] =
    (
      configFile,
      fieldNamesConfig,
      timestampConfig,
      grepConfig,
      filterConfig,
      formatIn,
      formatOut,
      showEmptyFields
    ).mapN {
      case (
            configFile,
            fieldNamesConfig,
            timestampConfig,
            grepConfig,
            filterConfig,
            formatIn,
            formatOut,
            showEmptyFields
          ) =>
        Config(
          configFile = configFile,
          fieldNames = fieldNamesConfig,
          timestamp = timestampConfig,
          grep = grepConfig,
          filter = filterConfig,
          formatIn = formatIn,
          formatOut = formatOut,
          showEmptyFields = showEmptyFields
        )
    }

  val command: Command[Config] = Command(
    name = "json-log-viewer",
    header = "Print json logs in human-readable form",
    helpFlag = true
  ).apply(config)
}
