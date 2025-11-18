package ru.d10xa.jsonlogviewer.formatout

import munit.FunSuite
import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParsedLine

class ColorLineFormatterTest extends FunSuite {

  val standardConfig: ResolvedConfig = ResolvedConfig(
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
    fuzzyInclude = None,
    fuzzyExclude = None,
    excludeFields = None,
    timestampAfter = None,
    timestampBefore = None,
    grep = List.empty,
    showEmptyFields = false
  )

  val configWithEmptyFields: ResolvedConfig = standardConfig.copy(
    showEmptyFields = true
  )

  test("shouldHideEmptyFields when showEmptyFields=false") {
    val formatter = new ColorLineFormatter(standardConfig, None, None)

    val parseResult = ParseResult(
      raw = "raw log line",
      parsed = Some(
        ParsedLine(
          timestamp = Some("2023-01-01T12:00:00Z"),
          level = Some("INFO"),
          message = Some("Test message"),
          stackTrace = None,
          loggerName = Some("TestLogger"),
          threadName = Some("main"),
          otherAttributes = Map(
            "empty_string" -> "",
            "null_value" -> "null",
            "normal_value" -> "test",
            "empty_object" -> "{}",
            "empty_array" -> "[]"
          )
        )
      ),
      middle = "middle part",
      prefix = None,
      postfix = None
    )

    val formatted = formatter.formatLine(parseResult).plainText

    assert(formatted.contains("normal_value"), "Normal value should be present")
    assert(
      !formatted.contains("empty_string"),
      "Empty string should not be present"
    )
    assert(
      !formatted.contains("null_value"),
      "Null value should not be present"
    )
    assert(
      !formatted.contains("empty_object"),
      "Empty object should not be present"
    )
    assert(
      !formatted.contains("empty_array"),
      "Empty array should not be present"
    )
  }

  test("shouldShowEmptyFields when showEmptyFields=true") {
    val formatter = new ColorLineFormatter(configWithEmptyFields, None, None)

    val parseResult = ParseResult(
      raw = "raw log line",
      parsed = Some(
        ParsedLine(
          timestamp = Some("2023-01-01T12:00:00Z"),
          level = Some("INFO"),
          message = Some("Test message"),
          stackTrace = None,
          loggerName = Some("TestLogger"),
          threadName = Some("main"),
          otherAttributes = Map(
            "empty_string" -> "",
            "null_value" -> "null",
            "normal_value" -> "test",
            "empty_object" -> "{}",
            "empty_array" -> "[]"
          )
        )
      ),
      middle = "middle part",
      prefix = None,
      postfix = None
    )

    val formatted = formatter.formatLine(parseResult).plainText

    assert(formatted.contains("normal_value"), "Normal value should be present")
    assert(formatted.contains("empty_string"), "Empty string should be present")
    assert(formatted.contains("null_value"), "Null value should be present")
    assert(formatted.contains("empty_object"), "Empty object should be present")
    assert(formatted.contains("empty_array"), "Empty array should be present")
  }
}
