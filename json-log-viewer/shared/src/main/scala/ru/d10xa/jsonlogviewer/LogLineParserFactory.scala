package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.csv.CsvLogLineParser
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser

object LogLineParserFactory {

  def createNonCsvParser(resolvedConfig: ResolvedConfig): LogLineParser = {
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    resolvedConfig.formatIn match {
      case Some(FormatIn.Logfmt) =>
        LogfmtLogLineParser(resolvedConfig)
      case Some(FormatIn.Csv) =>
        throw new IllegalStateException(
          "CSV format requires header line, use createCsvParser instead"
        )
      case Some(FormatIn.Json) | None =>
        JsonLogLineParser(resolvedConfig, jsonPrefixPostfix)
    }
  }

  def createCsvParser(
    resolvedConfig: ResolvedConfig,
    headerLine: String
  ): LogLineParser = {
    CsvLogLineParser(resolvedConfig, headerLine)
  }
}
