package ru.d10xa.jsonlogviewer

import munit.FunSuite
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.config.ResolvedConfig

class ParseResultKeysTest extends FunSuite {

  test("getByKey uses configured field names") {
    val standardConfig = ResolvedConfig(
      feedName = None,
      commands = List.empty,
      inlineInput = None,
      filter = None,
      formatIn = None,
      formatOut = None,
      fieldNames = FieldNamesConfig(
        timestampFieldName = "@timestamp",
        levelFieldName = "level",
        messageFieldName = "message",
        stackTraceFieldName = "stack_trace",
        loggerNameFieldName = "logger_name",
        threadNameFieldName = "thread_name"
      ),
      rawInclude = None,
      rawExclude = None,
      excludeFields = None,
      timestampAfter = None,
      timestampBefore = None,
      grep = List.empty
    )

    val customConfig = ResolvedConfig(
      feedName = None,
      commands = List.empty,
      inlineInput = None,
      filter = None,
      formatIn = None,
      formatOut = None,
      fieldNames = FieldNamesConfig(
        timestampFieldName = "ts",
        levelFieldName = "severity",
        messageFieldName = "text",
        stackTraceFieldName = "exception",
        loggerNameFieldName = "logger",
        threadNameFieldName = "thread"
      ),
      rawInclude = None,
      rawExclude = None,
      excludeFields = None,
      timestampAfter = None,
      timestampBefore = None,
      grep = List.empty
    )

    val parseResult = ParseResult(
      raw = "raw log line",
      parsed = Some(
        ParsedLine(
          timestamp = Some("2023-01-01T12:00:00Z"),
          level = Some("INFO"),
          message = Some("Test message"),
          stackTrace = Some("Error trace"),
          loggerName = Some("TestLogger"),
          threadName = Some("main"),
          otherAttributes = Map("custom_field" -> "custom_value")
        )
      ),
      middle = "middle part",
      prefix = Some("prefix"),
      postfix = Some("postfix")
    )

    val standardKeys = new ParseResultKeys(standardConfig)
    assertEquals(
      standardKeys.getByKey(parseResult, "@timestamp"),
      Some("2023-01-01T12:00:00Z")
    )
    assertEquals(standardKeys.getByKey(parseResult, "level"), Some("INFO"))
    assertEquals(
      standardKeys.getByKey(parseResult, "message"),
      Some("Test message")
    )
    assertEquals(
      standardKeys.getByKey(parseResult, "stack_trace"),
      Some("Error trace")
    )
    assertEquals(
      standardKeys.getByKey(parseResult, "logger_name"),
      Some("TestLogger")
    )
    assertEquals(
      standardKeys.getByKey(parseResult, "thread_name"),
      Some("main")
    )
    assertEquals(
      standardKeys.getByKey(parseResult, "custom_field"),
      Some("custom_value")
    )
    assertEquals(standardKeys.getByKey(parseResult, "prefix"), Some("prefix"))
    assertEquals(standardKeys.getByKey(parseResult, "postfix"), Some("postfix"))

    val customKeys = new ParseResultKeys(customConfig)
    assertEquals(
      customKeys.getByKey(parseResult, "ts"),
      Some("2023-01-01T12:00:00Z")
    )
    assertEquals(customKeys.getByKey(parseResult, "severity"), Some("INFO"))
    assertEquals(customKeys.getByKey(parseResult, "text"), Some("Test message"))
    assertEquals(
      customKeys.getByKey(parseResult, "exception"),
      Some("Error trace")
    )
    assertEquals(customKeys.getByKey(parseResult, "logger"), Some("TestLogger"))
    assertEquals(customKeys.getByKey(parseResult, "thread"), Some("main"))

    assertEquals(customKeys.getByKey(parseResult, "@timestamp"), Some("2023-01-01T12:00:00Z"))
    assertEquals(customKeys.getByKey(parseResult, "level"), Some("INFO"))
    assertEquals(customKeys.getByKey(parseResult, "message"), Some("Test message"))

    assertEquals(
      customKeys.getByKey(parseResult, "custom_field"),
      Some("custom_value")
    )
    assertEquals(customKeys.getByKey(parseResult, "prefix"), Some("prefix"))
    assertEquals(customKeys.getByKey(parseResult, "postfix"), Some("postfix"))
  }
}