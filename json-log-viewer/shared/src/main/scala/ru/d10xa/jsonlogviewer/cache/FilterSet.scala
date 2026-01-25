package ru.d10xa.jsonlogviewer.cache

import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.FilterComponents
import ru.d10xa.jsonlogviewer.LogLineParser

/** Pre-built filters and parser. Parser is None for CSV (requires header). */
final case class FilterSet(
  resolvedConfig: ResolvedConfig,
  components: FilterComponents,
  parser: Option[LogLineParser]
)
