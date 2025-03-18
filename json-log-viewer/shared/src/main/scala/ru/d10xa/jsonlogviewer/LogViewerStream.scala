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

            // First line of csv is header
            val formatIn = feed.formatIn.orElse(config.formatIn)
            if (formatIn.contains(FormatIn.Csv)) {
              processStreamWithCsvHeader(
                config,
                feedStream,
                configYamlRef,
                index
              )
            } else {
              processStream(config, feedStream, configYamlRef, index)
            }
          }
          Stream.emits(feedStreams).parJoin(feedStreams.size)
        case None =>
          // First line of csv is header
          if (config.formatIn.contains(FormatIn.Csv)) {
            processStreamWithCsvHeader(
              config,
              stdinLinesStream,
              configYamlRef,
              -1
            )
          } else {
            processStream(config, stdinLinesStream, configYamlRef, -1)
          }
      }

      finalStream
        .intersperse("\n")
        .append(Stream.emit("\n"))
    }

  private def processStreamWithCsvHeader(
    config: Config,
    lines: Stream[IO, String],
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    index: Int
  ): Stream[IO, String] =
    lines.pull.uncons1.flatMap {
      case Some((headerLine, rest)) =>
        val csvHeaderParser = CsvLogLineParser(config, headerLine)
        Stream
          .emit(csvHeaderParser)
          .flatMap { parser =>
            processStreamWithParser(config, rest, configYamlRef, index, parser)
          }
          .pull
          .echo
      case None =>
        Pull.done
    }.stream

  private def processStreamWithParser(
    config: Config,
    lines: Stream[IO, String],
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    index: Int,
    logLineParser: LogLineParser
  ): Stream[IO, String] =
    for {
      line <- lines
      optConfigYaml <- Stream.eval(configYamlRef.get)
      filter = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.filter))
        .orElse(config.filter)
      rawInclude = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.rawInclude))
      rawExclude = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.rawExclude))
      feedName = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.name))
      effectiveConfig = config.copy(
        filter = filter
      )
      timestampFilter = TimestampFilter()
      parseResultKeys = ParseResultKeys(effectiveConfig)
      logLineFilter = LogLineFilter(effectiveConfig, parseResultKeys)
      outputLineFormatter = effectiveConfig.formatOut match
        case Some(Config.FormatOut.Raw) => RawFormatter()
        case Some(Config.FormatOut.Pretty) | None =>
          ColorLineFormatter(effectiveConfig, feedName)
      evaluatedLine <- Stream
        .emit(line)
        .filter(rawFilter(_, rawInclude, rawExclude))
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
        .map(pr =>
          Try(outputLineFormatter.formatLine(pr)) match {
            case Success(formatted) => formatted.toString
            case Failure(_)         => pr.raw
          }
        )
    } yield evaluatedLine

  /** Processing for non-csv files
    */
  private def processStream(
    config: Config,
    lines: Stream[IO, String],
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    index: Int
  ): Stream[IO, String] =
    for {
      line <- lines
      optConfigYaml <- Stream.eval(configYamlRef.get)
      formatIn = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.formatIn))
        .orElse(config.formatIn)
      filter = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.filter))
        .orElse(config.filter)
      rawInclude = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.rawInclude))
      rawExclude = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.rawExclude))
      feedName = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.name))
      effectiveConfig = config.copy(
        filter = filter,
        formatIn = formatIn
      )
      timestampFilter = TimestampFilter()
      parseResultKeys = ParseResultKeys(effectiveConfig)
      logLineFilter = LogLineFilter(effectiveConfig, parseResultKeys)
      logLineParser = makeNonCsvLogLineParser(effectiveConfig, formatIn)
      outputLineFormatter = effectiveConfig.formatOut match
        case Some(Config.FormatOut.Raw) => RawFormatter()
        case Some(Config.FormatOut.Pretty) | None =>
          ColorLineFormatter(effectiveConfig, feedName)
      evaluatedLine <- Stream
        .emit(line)
        .filter(rawFilter(_, rawInclude, rawExclude))
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
        .map(pr =>
          Try(outputLineFormatter.formatLine(pr)) match {
            case Success(formatted) => formatted.toString
            case Failure(_)         => pr.raw
          }
        )
    } yield evaluatedLine

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
          "method makeNonCsvLogLineParserCSV not support csv"
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
