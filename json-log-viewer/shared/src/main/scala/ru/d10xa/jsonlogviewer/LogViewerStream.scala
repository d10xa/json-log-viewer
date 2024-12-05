package ru.d10xa.jsonlogviewer

import fs2.*
import fs2.io.*
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.formatout.ColorLineFormatter
import ru.d10xa.jsonlogviewer.formatout.RawFormatter
object LogViewerStream {

  def stream[F[_]](
    config: Config,
    logLineParser: LogLineParser
  ): Pipe[F, String, String] = stream =>
    val timestampFilter = TimestampFilter()
    val outputLineFormatter = config.formatOut match
      case Some(Config.FormatOut.Raw) => RawFormatter()
      case Some(Config.FormatOut.Pretty) | None => ColorLineFormatter(config)
    
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
