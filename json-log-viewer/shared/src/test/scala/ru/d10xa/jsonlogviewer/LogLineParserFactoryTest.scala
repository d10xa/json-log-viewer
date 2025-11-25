package ru.d10xa.jsonlogviewer

import munit.FunSuite
import ru.d10xa.jsonlogviewer.csv.CsvLogLineParser
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser

class LogLineParserFactoryTest extends FunSuite with TestResolvedConfig {

  test("createNonCsvParser should create JsonLogLineParser for Json format") {
    val config = baseResolvedConfig.copy(formatIn = Some(Config.FormatIn.Json))
    val parser = LogLineParserFactory.createNonCsvParser(config)

    assert(
      parser.isInstanceOf[JsonLogLineParser],
      "Should create JsonLogLineParser for Json format"
    )
  }

  test("createNonCsvParser should create JsonLogLineParser when formatIn is None") {
    val config = baseResolvedConfig.copy(formatIn = None)
    val parser = LogLineParserFactory.createNonCsvParser(config)

    assert(
      parser.isInstanceOf[JsonLogLineParser],
      "Should create JsonLogLineParser when formatIn is None (default)"
    )
  }

  test("createNonCsvParser should create LogfmtLogLineParser for Logfmt format") {
    val config = baseResolvedConfig.copy(formatIn = Some(Config.FormatIn.Logfmt))
    val parser = LogLineParserFactory.createNonCsvParser(config)

    assert(
      parser.isInstanceOf[LogfmtLogLineParser],
      "Should create LogfmtLogLineParser for Logfmt format"
    )
  }

  test("createNonCsvParser should throw IllegalStateException for Csv format") {
    val config = baseResolvedConfig.copy(formatIn = Some(Config.FormatIn.Csv))

    intercept[IllegalStateException] {
      LogLineParserFactory.createNonCsvParser(config)
    }
  }

  test("createCsvParser should create CsvLogLineParser") {
    val headerLine = "timestamp,level,message"
    val parser = LogLineParserFactory.createCsvParser(baseResolvedConfig, headerLine)

    assert(
      parser.isInstanceOf[CsvLogLineParser],
      "Should create CsvLogLineParser"
    )
  }

  test("createCsvParser should use provided header line") {
    val headerLine = "ts,severity,msg"
    val parser = LogLineParserFactory.createCsvParser(baseResolvedConfig, headerLine)

    // Parse a CSV line to verify header was used
    val csvLine = "2023-01-01T10:00:00Z,INFO,Test message"
    val result = parser.parse(csvLine)

    assert(
      result.parsed.isDefined,
      "Should parse CSV line using provided header"
    )
  }
}
