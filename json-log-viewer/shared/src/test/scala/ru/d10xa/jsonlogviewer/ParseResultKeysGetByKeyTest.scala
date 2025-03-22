package ru.d10xa.jsonlogviewer

import munit.FunSuite
import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig

class ParseResultKeysGetByKeyTest extends FunSuite {

  private val customConfig = ResolvedConfig(
    feedName = None,
    commands = List.empty,
    inlineInput = None,
    filter = None,
    formatIn = None,
    formatOut = None,
    fieldNames = FieldNamesConfig(
      timestampFieldName = "ts",
      levelFieldName = "severity",
      messageFieldName = "msg",
      stackTraceFieldName = "trace",
      loggerNameFieldName = "logger",
      threadNameFieldName = "thread"
    ),
    rawInclude = None,
    rawExclude = None,
    excludeFields = None,
    timestampAfter = None,
    timestampBefore = None,
    grep = List.empty,
    showEmptyFields = false,
  )

  private val parseResult = ParseResult(
    raw = "raw log line",
    parsed = Some(
      ParsedLine(
        timestamp = Some("2023-01-01T12:00:00Z"),
        level = Some("ERROR"),
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

  private val parseResultKeys = new ParseResultKeys(customConfig)

  test("should get level value by standard field name even when renamed") {
    assertEquals(
      parseResultKeys.getByKey(parseResult, "level"),
      Some("ERROR"),
      "Should get value by standard field name 'level' even when renamed to 'severity'"
    )
  }

  test("should get level value by custom field name") {
    assertEquals(
      parseResultKeys.getByKey(parseResult, "severity"),
      Some("ERROR"),
      "Should get value by custom field name 'severity'"
    )
  }

  test("should get message by standard name") {
    assertEquals(
      parseResultKeys.getByKey(parseResult, "message"),
      Some("Test message"),
      "Should get message by standard name"
    )
  }

  test("should get message by custom name") {
    assertEquals(
      parseResultKeys.getByKey(parseResult, "msg"),
      Some("Test message"),
      "Should get message by custom name"
    )
  }

  test("should get timestamp by standard name") {
    assertEquals(
      parseResultKeys.getByKey(parseResult, "timestamp"),
      Some("2023-01-01T12:00:00Z"),
      "Should get timestamp by standard name"
    )
  }

  test("should get timestamp by custom name") {
    assertEquals(
      parseResultKeys.getByKey(parseResult, "ts"),
      Some("2023-01-01T12:00:00Z"),
      "Should get timestamp by custom name"
    )
  }

  test("should get stack trace by standard name") {
    assertEquals(
      parseResultKeys.getByKey(parseResult, "stack_trace"),
      Some("Error trace"),
      "Should get stack trace by standard name"
    )
  }

  test("should get stack trace by custom name") {
    assertEquals(
      parseResultKeys.getByKey(parseResult, "trace"),
      Some("Error trace"),
      "Should get stack trace by custom name"
    )
  }
}
