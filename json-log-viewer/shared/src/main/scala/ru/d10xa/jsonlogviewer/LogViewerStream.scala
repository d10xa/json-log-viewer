package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.*
import fs2.Pull
import ru.d10xa.jsonlogviewer.csv.CsvLogLineParser
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.formatout.ColorLineFormatter
import ru.d10xa.jsonlogviewer.formatout.RawFormatter
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser
import ru.d10xa.jsonlogviewer.shell.ShellImpl

import scala.util.matching.Regex
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object LogViewerStream {

  private val stdinLinesStream: Stream[IO, String] =
    new StdInLinesStreamImpl().stdinLinesStream

  def stream(
    config: Config,
    configYamlRef: Ref[IO, Option[ConfigYaml]]
  ): Stream[IO, String] =
    Stream.eval(configYamlRef.get).flatMap { configYamlOpt =>
      val feedsOpt: Option[List[Feed]] =
        configYamlOpt.flatMap(_.feeds).filter(_.nonEmpty)

      val finalStream = feedsOpt match {
        case Some(feeds) =>
          val feedStreams = feeds.zipWithIndex.map { (feed, index) =>
            val feedStream: Stream[IO, String] =
              commandsAndInlineInputToStream(feed.commands, feed.inlineInput)

            createProcessStream(
              config = config,
              lines = feedStream,
              configYamlRef = configYamlRef,
              index = index,
              initialFormatIn = feed.formatIn.orElse(config.formatIn)
            )
          }
          Stream.emits(feedStreams).parJoin(feedStreams.size)
        case None =>
          createProcessStream(
            config = config,
            lines = stdinLinesStream,
            configYamlRef = configYamlRef,
            index = -1,
            initialFormatIn = config.formatIn
          )
      }

      finalStream
        .intersperse("\n")
        .append(Stream.emit("\n"))
    }

  private def createProcessStream(
    config: Config,
    lines: Stream[IO, String],
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    index: Int,
    initialFormatIn: Option[FormatIn]
  ): Stream[IO, String] =
    if (initialFormatIn.contains(FormatIn.Csv)) {
      lines.pull.uncons1.flatMap {
        case Some((headerLine, rest)) =>
          val csvHeaderParser = CsvLogLineParser(config, headerLine)
          processStreamWithEffectiveConfig(
            config = config,
            lines = rest,
            configYamlRef = configYamlRef,
            index = index,
            parser = Some(csvHeaderParser)
          ).pull.echo
        case None =>
          Pull.done
      }.stream
    } else {
      processStreamWithEffectiveConfig(
        config = config,
        lines = lines,
        configYamlRef = configYamlRef,
        index = index,
        parser = None
      )
    }

  private def processStreamWithEffectiveConfig(
    config: Config,
    lines: Stream[IO, String],
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    index: Int,
    parser: Option[LogLineParser]
  ): Stream[IO, String] =
    for {
      line <- lines
      optConfigYaml <- Stream.eval(configYamlRef.get)

      feedConfig = extractFeedConfig(optConfigYaml, index)

      effectiveConfig = config.copy(
        filter = feedConfig.filter.orElse(config.filter),
        formatIn = feedConfig.formatIn.orElse(config.formatIn)
      )

      timestampFilter = TimestampFilter()
      parseResultKeys = ParseResultKeys(effectiveConfig)
      logLineFilter = LogLineFilter(effectiveConfig, parseResultKeys)

      logLineParser = parser.getOrElse(
        makeNonCsvLogLineParser(effectiveConfig, feedConfig.formatIn)
      )

      outputLineFormatter = effectiveConfig.formatOut match {
        case Some(Config.FormatOut.Raw) => RawFormatter()
        case Some(Config.FormatOut.Pretty) | None =>
          ColorLineFormatter(effectiveConfig, feedConfig.feedName, feedConfig.excludeFields)
      }

      evaluatedLine <- Stream
        .emit(line)
        .filter(rawFilter(_, feedConfig.rawInclude, feedConfig.rawExclude))
        .map(logLineParser.parse)
        .filter(logLineFilter.grep)
        .filter(logLineFilter.logLineQueryPredicate)
        .through(
          timestampFilter.filterTimestampAfter(effectiveConfig.timestamp.after)
        )
        .through(
          timestampFilter.filterTimestampBefore(
            effectiveConfig.timestamp.before
          )
        )
        .map(formatWithSafety(_, outputLineFormatter))
    } yield evaluatedLine

  private def formatWithSafety(
    parseResult: ParseResult,
    formatter: OutputLineFormatter
  ): String =
    Try(formatter.formatLine(parseResult)) match {
      case Success(formatted) => formatted.toString
      case Failure(_)         => parseResult.raw
    }

  // TODO
  private case class FeedConfig(
    feedName: Option[String],
    filter: Option[ru.d10xa.jsonlogviewer.query.QueryAST],
    formatIn: Option[FormatIn],
    rawInclude: Option[List[String]],
    rawExclude: Option[List[String]],
    excludeFields: Option[List[String]]
  )

  private def extractFeedConfig(
                                 optConfigYaml: Option[ConfigYaml],
                                 index: Int
                               ): FeedConfig = {
    val feedOpt = optConfigYaml
      .flatMap(_.feeds)
      .flatMap(_.lift(index))

    FeedConfig(
      feedName = feedOpt.flatMap(_.name),
      filter = feedOpt.flatMap(_.filter),
      formatIn = feedOpt.flatMap(_.formatIn),
      rawInclude = feedOpt.flatMap(_.rawInclude),
      rawExclude = feedOpt.flatMap(_.rawExclude),
      excludeFields = feedOpt.flatMap(_.excludeFields)
    )
  }

  private def commandsAndInlineInputToStream(
    commands: List[String],
    inlineInput: Option[String]
  ): Stream[IO, String] =
    new ShellImpl().mergeCommandsAndInlineInput(commands, inlineInput)

  def makeNonCsvLogLineParser(
    config: Config,
    optFormatIn: Option[FormatIn]
  ): LogLineParser = {
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    optFormatIn match {
      case Some(FormatIn.Logfmt) => LogfmtLogLineParser(config)
      case Some(FormatIn.Csv) =>
        throw new IllegalStateException(
          "method makeNonCsvLogLineParser does not support csv"
        )
      case _ => JsonLogLineParser(config, jsonPrefixPostfix)
    }
  }

  def rawFilter(
    str: String,
    include: Option[List[String]],
    exclude: Option[List[String]]
  ): Boolean = {
    val includeRegexes: List[Regex] =
      include.getOrElse(Nil).map(_.r)
    val excludeRegexes: List[Regex] =
      exclude.getOrElse(Nil).map(_.r)
    val includeMatches = includeRegexes.isEmpty || includeRegexes.exists(
      _.findFirstIn(str).isDefined
    )
    val excludeMatches = excludeRegexes.forall(_.findFirstIn(str).isEmpty)
    includeMatches && excludeMatches
  }
}
