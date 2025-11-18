package ru.d10xa.jsonlogviewer

import munit.FunSuite
import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.decline.FieldNamesConfig

class FuzzyFilterTest extends FunSuite {

  private def createResolvedConfig(
    fuzzyInclude: Option[List[String]] = None,
    fuzzyExclude: Option[List[String]] = None
  ): ResolvedConfig = ResolvedConfig(
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
    fuzzyInclude = fuzzyInclude,
    fuzzyExclude = fuzzyExclude,
    excludeFields = None,
    timestampAfter = None,
    timestampBefore = None,
    grep = List.empty,
    showEmptyFields = false
  )

  private def createParseResult(
    level: Option[String] = None,
    message: Option[String] = None,
    otherAttributes: Map[String, String] = Map.empty
  ): ParseResult = {
    val parsed = ParsedLine(
      timestamp = Some("2024-01-01T10:00:00Z"),
      level = level,
      message = message,
      stackTrace = None,
      loggerName = None,
      threadName = None,
      otherAttributes = otherAttributes
    )
    ParseResult(
      raw = """{"level":"ERROR","message":"Connection timeout"}""",
      parsed = Some(parsed),
      middle = "",
      prefix = None,
      postfix = None
    )
  }

  test("should match pattern across different fields") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("error timeout"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("Connection timeout")
    )

    assert(filter.test(parseResult))
  }

  test("should be case insensitive") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("error timeout"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("TIMEOUT occurred")
    )

    assert(filter.test(parseResult))
  }

  test("should work with word order independence") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("timeout error"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("Connection timeout")
    )

    assert(filter.test(parseResult))
  }

  test("should search in otherAttributes") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("error timeout"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("INFO"),
      message = Some("Processing request"),
      otherAttributes = Map("status" -> "error", "duration" -> "timeout")
    )

    assert(filter.test(parseResult))
  }

  test("fuzzyExclude should exclude matches") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("error timeout")),
      fuzzyExclude = Some(List("retry succeeded"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("Connection timeout but retry succeeded")
    )

    assert(!filter.test(parseResult))
  }

  test("should pass when fuzzyInclude is empty") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List.empty)
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("Any message")
    )

    assert(filter.test(parseResult))
  }

  test("should pass when fuzzyInclude is None") {
    val config = createResolvedConfig(
      fuzzyInclude = None
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("Any message")
    )

    assert(filter.test(parseResult))
  }

  test("should support multiple include patterns (OR logic)") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("database timeout", "connection refused"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult1 = createParseResult(
      level = Some("ERROR"),
      message = Some("Database query timeout")
    )
    assert(filter.test(parseResult1))

    val parseResult2 = createParseResult(
      level = Some("ERROR"),
      message = Some("Connection refused by server")
    )
    assert(filter.test(parseResult2))

    val parseResult3 = createParseResult(
      level = Some("ERROR"),
      message = Some("Unknown error")
    )
    assert(!filter.test(parseResult3))
  }

  test("should support multiple exclude patterns (AND NOT logic)") {
    val config = createResolvedConfig(
      fuzzyExclude = Some(List("test debug", "health check"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult1 = createParseResult(
      level = Some("INFO"),
      message = Some("Test environment debug mode")
    )
    assert(!filter.test(parseResult1))

    val parseResult2 = createParseResult(
      level = Some("INFO"),
      message = Some("Health check succeeded")
    )
    assert(!filter.test(parseResult2))

    val parseResult3 = createParseResult(
      level = Some("ERROR"),
      message = Some("Production error")
    )
    assert(filter.test(parseResult3))
  }

  test("should handle punctuation in log messages") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("database query failed"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("ERROR: database.query() failed - connection lost")
    )

    assert(filter.test(parseResult))
  }

  test("should support partial token matching") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("timeout error"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("Connection timeouts and errors occurred")
    )

    assert(filter.test(parseResult))
  }

  test("should handle missing parsed data by falling back to raw") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("error timeout"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = ParseResult(
      raw = """ERROR: Connection timeout""",
      parsed = None,
      middle = "",
      prefix = None,
      postfix = None
    )

    assert(filter.test(parseResult))
  }

  test("should not match when pattern words are missing") {
    val config = createResolvedConfig(
      fuzzyInclude = Some(List("database timeout user"))
    )
    val filter = new FuzzyFilter(config)

    val parseResult = createParseResult(
      level = Some("ERROR"),
      message = Some("Database timeout occurred")
      // Missing "user"
    )

    assert(!filter.test(parseResult))
  }
}
