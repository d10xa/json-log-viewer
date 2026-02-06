package ru.d10xa.jsonlogviewer.decline

import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.time.*
import com.monovore.decline.Opts
import java.time.ZonedDateTime
import ru.d10xa.jsonlogviewer.decline.Config.ConfigGrep
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatOut
import ru.d10xa.jsonlogviewer.decline.ConfigGrepValidator.given
import ru.d10xa.jsonlogviewer.decline.FormatInValidator.given
import ru.d10xa.jsonlogviewer.decline.FormatOutValidator.given
import ru.d10xa.jsonlogviewer.query.QueryAST

object DeclineOpts {

  private def fieldOpt(
    name: String,
    fieldDescription: String,
    default: String
  ): Opts[String] =
    Opts
      .option[String](
        name,
        help = s"Override default $fieldDescription field name"
      )
      .withDefault(default)

  // Timestamp filter options
  val timestampAfter: Opts[Option[ZonedDateTime]] =
    Opts
      .option[ZonedDateTime](
        "timestamp-after",
        "Show only entries after this timestamp"
      )
      .orNone
  val timestampBefore: Opts[Option[ZonedDateTime]] =
    Opts
      .option[ZonedDateTime](
        "timestamp-before",
        "Show only entries before this timestamp"
      )
      .orNone

  // Field name options
  val timestampField: Opts[String] =
    fieldOpt("timestamp-field", "timestamp", "@timestamp")
  val levelField: Opts[String] =
    fieldOpt("level-field", "level", "level")
  val messageField: Opts[String] =
    fieldOpt("message-field", "message", "message")
  val stackTraceField: Opts[String] =
    fieldOpt("stack-trace-field", "stack trace", "stack_trace")
  val loggerNameField: Opts[String] =
    fieldOpt("logger-name-field", "logger name", "logger_name")
  val threadNameField: Opts[String] =
    fieldOpt("thread-name-field", "thread name", "thread_name")

  val fieldNamesConfig: Opts[FieldNamesConfig] =
    (
      timestampField,
      levelField,
      messageField,
      stackTraceField,
      loggerNameField,
      threadNameField
    ).mapN(FieldNamesConfig.apply)

  val grepConfig: Opts[List[ConfigGrep]] = Opts
    .options[ConfigGrep](
      "grep",
      help = "Filter by key:value regex"
    )
    .orEmpty

  val filterConfig: Opts[Option[QueryAST]] = Opts
    .option[String]("filter", "sql expression")
    .mapValidated(QueryASTValidator.toValidatedQueryAST)
    .orNone

  val formatIn: Opts[Option[FormatIn]] = Opts
    .option[FormatIn]("format-in", help = "Input format")
    .orNone

  val formatOut: Opts[Option[FormatOut]] = Opts
    .option[FormatOut]("format-out", help = "Output format")
    .orNone

  val timestampConfig: Opts[TimestampConfig] =
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
    ).mapN(Config.apply)

  val command: Command[Config] = Command(
    name = "json-log-viewer",
    header = "Print json logs in human-readable form",
    helpFlag = true
  ).apply(config)
}
