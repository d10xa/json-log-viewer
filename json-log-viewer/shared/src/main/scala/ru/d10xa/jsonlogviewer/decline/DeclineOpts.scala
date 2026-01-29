package ru.d10xa.jsonlogviewer.decline

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
    Opts.option[ZonedDateTime]("timestamp-after", "").orNone
  val timestampBefore: Opts[Option[ZonedDateTime]] =
    Opts.option[ZonedDateTime]("timestamp-before", "").orNone

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

  def fieldNamesConfig: Opts[FieldNamesConfig] =
    (
      timestampField,
      levelField,
      messageField,
      stackTraceField,
      loggerNameField,
      threadNameField
    ).mapN(FieldNamesConfig.apply)

  val grepConfig: Opts[List[ConfigGrep]] = Opts
    .options[String]("grep", "", metavar = "key:value")
    .mapValidated(lines =>
      lines.traverse(ConfigGrepValidator.toValidatedConfigGrep)
    )
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
