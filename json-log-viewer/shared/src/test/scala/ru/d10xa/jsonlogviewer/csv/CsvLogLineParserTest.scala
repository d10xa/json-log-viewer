package ru.d10xa.jsonlogviewer.csv

import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.config.ResolvedConfig

class CsvLogLineParserTest extends FunSuite {
  val config: ResolvedConfig = ResolvedConfig(
    feedName = None,
    commands = List.empty,
    inlineInput = None,
    fieldNames = FieldNamesConfig(
      timestampFieldName = "@timestamp",
      levelFieldName = "level",
      messageFieldName = "message",
      stackTraceFieldName = "stack_trace",
      loggerNameFieldName = "logger_name",
      threadNameFieldName = "thread_name"
    ),
    filter = None,
    formatIn = Some(Config.FormatIn.Csv),
    formatOut = None,
    rawInclude = None,
    rawExclude = None,
    excludeFields = None,
    timestampAfter = None,
    timestampBefore = None,
    grep = List.empty,
    showEmptyFields = false
  )

  test("parse CSV log line with standard headers") {
    val headerLine =
      "@timestamp,level,message,logger_name,thread_name,stack_trace"
    val logLine = "2023-01-01T12:00:00Z,INFO,Test message,Logger,main,null"

    val parser = CsvLogLineParser(config, headerLine)
    val result = parser.parse(logLine)

    assertEquals(
      result.parsed.flatMap(_.timestamp),
      Some("2023-01-01T12:00:00Z")
    )
    assertEquals(result.parsed.flatMap(_.level), Some("INFO"))
    assertEquals(result.parsed.flatMap(_.message), Some("Test message"))
    assertEquals(result.parsed.flatMap(_.loggerName), Some("Logger"))
    assertEquals(result.parsed.flatMap(_.threadName), Some("main"))
    assertEquals(result.parsed.flatMap(_.stackTrace), Some("null"))
    assert(result.parsed.exists(_.otherAttributes.isEmpty))
  }

  test("parse CSV log line with custom headers") {
    val headerLine = "@timestamp,level,message,custom_field"
    val logLine = "2023-01-01T12:00:00Z,INFO,Test message,custom value"

    val parser = CsvLogLineParser(config, headerLine)
    val result = parser.parse(logLine)

    assertEquals(
      result.parsed.flatMap(_.timestamp),
      Some("2023-01-01T12:00:00Z")
    )
    assertEquals(result.parsed.flatMap(_.level), Some("INFO"))
    assertEquals(result.parsed.flatMap(_.message), Some("Test message"))
    assertEquals(
      result.parsed.exists(_.otherAttributes.contains("custom_field")),
      true
    )
    assertEquals(
      result.parsed.flatMap(_.otherAttributes.get("custom_field")),
      Some("custom value")
    )
  }

  test("parse CSV log line with spaces in header") {
    val headerLine =
      "\"@timestamp\",\"log level\",\"message text\",\"logger name\""
    val logLine = "2023-01-01T12:00:00Z,INFO,Test message,Logger"

    val parser = CsvLogLineParser(config, headerLine)
    val result = parser.parse(logLine)

    assertEquals(
      result.parsed.flatMap(_.timestamp),
      Some("2023-01-01T12:00:00Z")
    )
    assertEquals(result.parsed.flatMap(_.level), None)
    assertEquals(result.parsed.flatMap(_.message), None)
    assertEquals(result.parsed.flatMap(_.loggerName), None)

    assertEquals(
      result.parsed.exists(_.otherAttributes.contains("log level")),
      true
    )
    assertEquals(
      result.parsed.flatMap(_.otherAttributes.get("log level")),
      Some("INFO")
    )
    assertEquals(
      result.parsed.exists(_.otherAttributes.contains("message text")),
      true
    )
    assertEquals(
      result.parsed.flatMap(_.otherAttributes.get("message text")),
      Some("Test message")
    )
    assertEquals(
      result.parsed.exists(_.otherAttributes.contains("logger name")),
      true
    )
    assertEquals(
      result.parsed.flatMap(_.otherAttributes.get("logger name")),
      Some("Logger")
    )
  }

  test("parse CSV log line with missing fields") {
    val headerLine =
      "@timestamp,level,message,logger_name,thread_name,stack_trace,custom_field"
    val logLine = "2023-01-01T12:00:00Z,INFO,Test message,Logger"

    val parser = CsvLogLineParser(config, headerLine)
    val result = parser.parse(logLine)

    assertEquals(
      result.parsed.flatMap(_.timestamp),
      Some("2023-01-01T12:00:00Z")
    )
    assertEquals(result.parsed.flatMap(_.level), Some("INFO"))
    assertEquals(result.parsed.flatMap(_.message), Some("Test message"))
    assertEquals(result.parsed.flatMap(_.loggerName), Some("Logger"))
    assertEquals(result.parsed.flatMap(_.threadName), None)
    assertEquals(result.parsed.flatMap(_.stackTrace), None)
    assertEquals(
      result.parsed.exists(_.otherAttributes.contains("custom_field")),
      false
    )
  }

  test("parse CSV log line with more values than headers") {
    val headerLine = "@timestamp,level,message"
    val logLine = "2023-01-01T12:00:00Z,INFO,Test message,Extra,Values"

    val parser = CsvLogLineParser(config, headerLine)
    val result = parser.parse(logLine)

    assertEquals(
      result.parsed.flatMap(_.timestamp),
      Some("2023-01-01T12:00:00Z")
    )
    assertEquals(result.parsed.flatMap(_.level), Some("INFO"))
    assertEquals(result.parsed.flatMap(_.message), Some("Test message"))
    assert(result.parsed.exists(_.otherAttributes.isEmpty))
  }
}