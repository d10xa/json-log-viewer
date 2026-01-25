package ru.d10xa.jsonlogviewer

import cats.effect.IO
import fs2.Stream
import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object FilterPipeline {

  // Filter order: rawFilter -> parse -> grep -> query -> fuzzy -> timestamp -> format
  def applyFilters(
    stream: Stream[IO, String],
    parser: LogLineParser,
    components: FilterComponents,
    resolvedConfig: ResolvedConfig
  ): Stream[IO, String] =
    stream
      .filter(
        rawFilter(_, resolvedConfig.rawInclude, resolvedConfig.rawExclude)
      )
      .map(parser.parse)
      .filter(components.logLineFilter.grep)
      .filter(components.logLineFilter.logLineQueryPredicate)
      .filter(components.fuzzyFilter.test)
      .through(
        components.timestampFilter.filterTimestampAfter(
          resolvedConfig.timestampAfter
        )
      )
      .through(
        components.timestampFilter.filterTimestampBefore(
          resolvedConfig.timestampBefore
        )
      )
      .map(formatWithSafety(_, components.outputLineFormatter))

  private def rawFilter(
    str: String,
    include: Option[List[String]],
    exclude: Option[List[String]]
  ): Boolean = {
    import scala.util.matching.Regex
    val includeRegexes: List[Regex] = include.getOrElse(Nil).map(_.r)
    val excludeRegexes: List[Regex] = exclude.getOrElse(Nil).map(_.r)
    val includeMatches = includeRegexes.isEmpty || includeRegexes.exists(
      _.findFirstIn(str).isDefined
    )
    val excludeMatches = excludeRegexes.forall(_.findFirstIn(str).isEmpty)
    includeMatches && excludeMatches
  }

  private def formatWithSafety(
    parseResult: ParseResult,
    formatter: OutputLineFormatter
  ): String =
    Try(formatter.formatLine(parseResult)) match {
      case Success(formatted) => formatted.toString
      case Failure(_)         => parseResult.raw
    }
}
