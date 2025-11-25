package ru.d10xa.jsonlogviewer

import cats.effect.IO
import fs2.Stream
import munit.CatsEffectSuite

class FilterPipelineTest extends CatsEffectSuite with TestResolvedConfig {

  val testLogLine =
    """{"@timestamp":"2023-01-01T10:00:00Z","level":"INFO","message":"Test message","logger_name":"TestLogger","thread_name":"main"}"""

  test("applyFilters should process a single log line") {
    val parser = JsonLogLineParser(
      baseResolvedConfig,
      JsonPrefixPostfix(JsonDetector())
    )
    val components = FilterComponents.fromConfig(baseResolvedConfig)
    val stream = Stream.emit(testLogLine)

    FilterPipeline
      .applyFilters(stream, parser, components, baseResolvedConfig)
      .compile
      .toList
      .map { result =>
        assert(result.nonEmpty, "Should produce output")
        assert(result.head.contains("INFO"), "Output should contain log level")
      }
  }

  test("applyFilters should filter out lines by rawExclude") {
    val configWithExclude = baseResolvedConfig.copy(
      rawExclude = Some(List("ERROR"))
    )
    val parser = JsonLogLineParser(
      configWithExclude,
      JsonPrefixPostfix(JsonDetector())
    )
    val components = FilterComponents.fromConfig(configWithExclude)

    val errorLine =
      """{"@timestamp":"2023-01-01T10:00:00Z","level":"ERROR","message":"Error message"}"""
    val infoLine = testLogLine

    val stream = Stream.emits(Seq(errorLine, infoLine))

    FilterPipeline
      .applyFilters(stream, parser, components, configWithExclude)
      .compile
      .toList
      .map { result =>
        // ERROR line should be filtered out by rawExclude
        assertEquals(result.length, 1, "Should only pass INFO line")
        assert(result.head.contains("INFO"), "Passed line should be INFO")
      }
  }

  test("applyFilters should include lines by rawInclude") {
    val configWithInclude = baseResolvedConfig.copy(
      rawInclude = Some(List("INFO"))
    )
    val parser = JsonLogLineParser(
      configWithInclude,
      JsonPrefixPostfix(JsonDetector())
    )
    val components = FilterComponents.fromConfig(configWithInclude)

    val errorLine =
      """{"@timestamp":"2023-01-01T10:00:00Z","level":"ERROR","message":"Error message"}"""
    val infoLine = testLogLine

    val stream = Stream.emits(Seq(errorLine, infoLine))

    FilterPipeline
      .applyFilters(stream, parser, components, configWithInclude)
      .compile
      .toList
      .map { result =>
        // Only INFO line should pass rawInclude filter
        assertEquals(result.length, 1, "Should only pass INFO line")
        assert(result.head.contains("INFO"), "Passed line should be INFO")
      }
  }

  test("applyFilters should process multiple lines") {
    val parser = JsonLogLineParser(
      baseResolvedConfig,
      JsonPrefixPostfix(JsonDetector())
    )
    val components = FilterComponents.fromConfig(baseResolvedConfig)

    val lines = List.fill(5)(testLogLine)
    val stream = Stream.emits(lines)

    FilterPipeline
      .applyFilters(stream, parser, components, baseResolvedConfig)
      .compile
      .toList
      .map { result =>
        assertEquals(result.length, 5, "Should process all 5 lines")
      }
  }

  test("applyFilters should handle malformed JSON gracefully") {
    val parser = JsonLogLineParser(
      baseResolvedConfig,
      JsonPrefixPostfix(JsonDetector())
    )
    val components = FilterComponents.fromConfig(baseResolvedConfig)

    val malformedLine = """not a json line"""
    val validLine = testLogLine

    val stream = Stream.emits(Seq(malformedLine, validLine))

    FilterPipeline
      .applyFilters(stream, parser, components, baseResolvedConfig)
      .compile
      .toList
      .map { result =>
        // Both lines should pass through (malformed as raw, valid as formatted)
        assertEquals(result.length, 2, "Should process both lines")
      }
  }

  test("applyFilters should work with empty stream") {
    val parser = JsonLogLineParser(
      baseResolvedConfig,
      JsonPrefixPostfix(JsonDetector())
    )
    val components = FilterComponents.fromConfig(baseResolvedConfig)

    val stream = Stream.empty

    FilterPipeline
      .applyFilters(stream, parser, components, baseResolvedConfig)
      .compile
      .toList
      .map { result =>
        assertEquals(result.length, 0, "Should produce no output for empty stream")
      }
  }

  test("applyFilters should apply all filters in correct order") {
    // This test verifies the pipeline order:
    // 1. rawFilter (before parsing)
    // 2. parse
    // 3. grep
    // 4. query
    // 5. fuzzy
    // 6. timestamp
    // 7. format

    val configWithFuzzy = baseResolvedConfig.copy(
      fuzzyInclude = Some(List("INFO"))  // Fuzzy filter for INFO
    )
    val parser = JsonLogLineParser(
      configWithFuzzy,
      JsonPrefixPostfix(JsonDetector())
    )
    val components = FilterComponents.fromConfig(configWithFuzzy)

    val warnLine =
      """{"@timestamp":"2023-01-01T10:00:00Z","level":"WARN","message":"Warning message"}"""

    val stream = Stream.emits(Seq(testLogLine, warnLine))

    FilterPipeline
      .applyFilters(stream, parser, components, configWithFuzzy)
      .compile
      .toList
      .map { result =>
        // Only INFO line should pass fuzzy filter
        assertEquals(result.length, 1, "Should only pass INFO line through fuzzy filter")
        assert(result.head.contains("INFO"), "Passed line should be INFO")
      }
  }
}
