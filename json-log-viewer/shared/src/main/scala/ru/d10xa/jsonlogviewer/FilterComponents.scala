package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.formatout.ColorLineFormatter
import ru.d10xa.jsonlogviewer.formatout.RawFormatter

final case class FilterComponents(
  timestampFilter: TimestampFilter,
  parseResultKeys: ParseResultKeys,
  logLineFilter: LogLineFilter,
  fuzzyFilter: FuzzyFilter,
  outputLineFormatter: OutputLineFormatter
)

object FilterComponents {

  def fromConfig(resolvedConfig: ResolvedConfig): FilterComponents = {
    val timestampFilter = TimestampFilter()
    val parseResultKeys = ParseResultKeys(resolvedConfig)
    val logLineFilter = LogLineFilter(resolvedConfig, parseResultKeys)
    val fuzzyFilter = new FuzzyFilter(resolvedConfig)

    val outputLineFormatter = resolvedConfig.formatOut match {
      case Some(Config.FormatOut.Raw) => RawFormatter()
      case Some(Config.FormatOut.Pretty) | None =>
        ColorLineFormatter(
          resolvedConfig,
          resolvedConfig.feedName,
          resolvedConfig.excludeFields
        )
    }

    FilterComponents(
      timestampFilter,
      parseResultKeys,
      logLineFilter,
      fuzzyFilter,
      outputLineFormatter
    )
  }
}
