package ru.d10xa.jsonlogviewer

import fs2.*
import fs2.io.*
object JsonLogViewerStream {

  def stream[F[_]](
    config: Config
  ): Pipe[F, String, String] = stream =>
    val timestampFilter = TimestampFilter()
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    val logLineParser = LogLineParser(config, jsonPrefixPostfix)
    val outputLineFormatter = ColorLineFormatter(config)
    val parseResultKeys = ParseResultKeys(config)
    val logLineFilter = LogLineFilter(config, parseResultKeys)
    stream
      .map(logLineParser.parse)
      .filter(logLineFilter.grep)
      .filter(logLineFilter.logLineQueryPredicate)
      .through(timestampFilter.filterTimestampAfter[F](config.timestamp.after))
      .through(
        timestampFilter.filterTimestampBefore[F](config.timestamp.before)
      )
      .map(outputLineFormatter.formatLine)
      .map(_.toString)
      .intersperse("\n")
      .append(Stream.emit("\n"))
}
