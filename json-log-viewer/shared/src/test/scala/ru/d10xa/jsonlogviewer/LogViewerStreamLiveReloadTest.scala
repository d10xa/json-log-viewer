package ru.d10xa.jsonlogviewer

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import munit.CatsEffectSuite
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.query.QueryCompiler

/** Tests that verify LogViewerStream's live config reload functionality
  */
class LogViewerStreamLiveReloadTest extends CatsEffectSuite {

  // Test logs
  val infoLog =
    """{"@timestamp":"2023-01-01T10:00:00Z","level":"INFO","message":"Test message","logger_name":"TestLogger","thread_name":"main"}"""
  val errorLog =
    """{"@timestamp":"2023-01-01T12:00:00Z","level":"ERROR","message":"Error message","logger_name":"TestLogger","thread_name":"main"}"""
  val customFormatLog =
    """{"ts":"2023-01-01T12:00:00Z","severity":"ERROR","msg":"Test message","logger_name":"TestLogger","thread_name":"main"}"""

  val baseConfig = Config(
    configFile = None,
    fieldNames = FieldNamesConfig(
      timestampFieldName = "@timestamp",
      levelFieldName = "level",
      messageFieldName = "message",
      stackTraceFieldName = "stack_trace",
      loggerNameFieldName = "logger_name",
      threadNameFieldName = "thread_name"
    ),
    timestamp = TimestampConfig(None, None),
    grep = List.empty,
    filter = None,
    formatIn = Some(Config.FormatIn.Json),
    formatOut = Some(Config.FormatOut.Raw),
    showEmptyFields = false
  )

  test("live reload should update filters during execution") {
    // Initial config with INFO filter
    val infoFilter = QueryCompiler("level = 'INFO'").toOption
    val initialConfig = ConfigYaml(
      fieldNames = None,
      showEmptyFields = None,
      feeds = Some(
        List(
          Feed(
            name = Some("test-feed"),
            commands = List.empty,
            inlineInput = None,
            filter = infoFilter,
            formatIn = None,
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None
          )
        )
      )
    )

    // Updated config with ERROR filter
    val errorFilter = QueryCompiler("level = 'ERROR'").toOption
    val updatedConfig = initialConfig.copy(
      feeds = initialConfig.feeds.map(_.map(_.copy(filter = errorFilter)))
    )

    for {
      // Create config ref with initial value
      configRef <- Ref.of[IO, Option[ConfigYaml]](Some(initialConfig))

      // Process logs with initial config (INFO filter)
      infoResults1 <- LogViewerStream
        .processLineWithRef(infoLog, configRef, baseConfig)
        .compile
        .toList
      errorResults1 <- LogViewerStream
        .processLineWithRef(errorLog, configRef, baseConfig)
        .compile
        .toList

      // Update config to use ERROR filter
      _ <- configRef.set(Some(updatedConfig))

      // Process logs with updated config (ERROR filter)
      infoResults2 <- LogViewerStream
        .processLineWithRef(infoLog, configRef, baseConfig)
        .compile
        .toList
      errorResults2 <- LogViewerStream
        .processLineWithRef(errorLog, configRef, baseConfig)
        .compile
        .toList
    } yield {
      // With initial config (INFO filter):
      assert(infoResults1.nonEmpty, "INFO log should pass initial INFO filter")
      assert(
        errorResults1.isEmpty,
        "ERROR log should not pass initial INFO filter"
      )

      // With updated config (ERROR filter):
      assert(
        infoResults2.isEmpty,
        "INFO log should not pass updated ERROR filter"
      )
      assert(
        errorResults2.nonEmpty,
        "ERROR log should pass updated ERROR filter"
      )
    }
  }

  test("live reload should update field names mapping") {
    // Initial config with standard field names
    val initialConfig = ConfigYaml(
      showEmptyFields = None,
      fieldNames = None,
      feeds = Some(
        List(
          Feed(
            name = Some("test-feed"),
            commands = List.empty,
            inlineInput = None,
            filter = None,
            formatIn = None,
            fieldNames = None,
            rawInclude = None,
            rawExclude = None,
            fuzzyInclude = None,
            fuzzyExclude = None,
            excludeFields = None,
            showEmptyFields = None
          )
        )
      )
    )

    // Updated config with custom field names
    val customFieldConfig = ConfigYaml(
      showEmptyFields = None,
      fieldNames = Some(
        FieldNames(
          timestamp = Some("ts"),
          level = Some("severity"),
          message = Some("msg"),
          stackTrace = None,
          loggerName = None,
          threadName = None
        )
      ),
      feeds = initialConfig.feeds
    )

    // Filter configurations that only work with correctly mapped fields
    val levelErrorFilter = QueryCompiler("level = 'ERROR'").toOption
    val severityErrorFilter = QueryCompiler("level = 'ERROR'").toOption
    val configWithLevelFilter = baseConfig.copy(filter = levelErrorFilter)
    val configWithSeverityFilter = baseConfig.copy(filter = severityErrorFilter)

    for {
      // Create config ref with initial value
      configRef <- Ref.of[IO, Option[ConfigYaml]](Some(initialConfig))

      // With initial config:
      // Standard log with level=ERROR should pass level filter
      standardLevelResults <- LogViewerStream
        .processLineWithRef(errorLog, configRef, configWithLevelFilter)
        .compile
        .toList
      // Custom log with severity=ERROR should not pass level filter (field not recognized)
      customLevelResults <- LogViewerStream
        .processLineWithRef(customFormatLog, configRef, configWithLevelFilter)
        .compile
        .toList
      // Custom log with severity=ERROR should not pass severity filter (field not mapped)
      customSeverityResults1 <- LogViewerStream
        .processLineWithRef(
          customFormatLog,
          configRef,
          configWithSeverityFilter
        )
        .compile
        .toList

      // Update config to map custom field names
      _ <- configRef.set(Some(customFieldConfig))

      // With updated config:
      // Standard log with level=ERROR should still pass level filter
      standardLevelResults2 <- LogViewerStream
        .processLineWithRef(errorLog, configRef, configWithLevelFilter)
        .compile
        .toList
      // Custom log should now pass severity filter (field properly mapped)
      customSeverityResults2 <- LogViewerStream
        .processLineWithRef(
          customFormatLog,
          configRef,
          configWithSeverityFilter
        )
        .compile
        .toList
    } yield {
      // Before field mapping update:
      assert(
        standardLevelResults.nonEmpty,
        "Standard log should pass level filter with initial config"
      )
      assert(
        customLevelResults.isEmpty,
        "Custom log should not pass level filter with initial config"
      )

      // After field mapping update:
      assert(
        standardLevelResults2.nonEmpty,
        "Standard log should still pass level filter after update"
      )
      assert(
        customSeverityResults2.nonEmpty,
        "Custom log should pass severity filter after field mapping update"
      )
    }
  }
}
