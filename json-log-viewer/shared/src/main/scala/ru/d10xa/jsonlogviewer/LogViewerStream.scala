package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.*
import fs2.Pull
import ru.d10xa.jsonlogviewer.config.ConfigResolver
import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.csv.CsvLogLineParser
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.formatout.ColorLineFormatter
import ru.d10xa.jsonlogviewer.formatout.RawFormatter
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser
import ru.d10xa.jsonlogviewer.shell.Shell
import ru.d10xa.jsonlogviewer.shell.ShellImpl
import scala.util.matching.Regex
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object LogViewerStream {

  def stream(
    config: Config,
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    stdinStream: StdInLinesStream,
    shell: Shell
  ): Stream[IO, String] = {
    def processStreamWithConfig(
      inputStream: Stream[IO, String],
      resolvedConfig: ResolvedConfig
    ): Stream[IO, String] =
      if (resolvedConfig.formatIn.contains(FormatIn.Csv)) {
        createCsvProcessStream(resolvedConfig, inputStream)
      } else {
        inputStream.flatMap { line =>
          Stream.eval(configYamlRef.get).flatMap { currentConfigYaml =>
            processLineWithConfig(line, currentConfigYaml, config)
          }
        }
      }

    Stream.eval(configYamlRef.get).flatMap { initialConfigYaml =>
      val resolvedConfigs = ConfigResolver.resolve(config, initialConfigYaml)

      val finalStream = if (resolvedConfigs.isEmpty) {
        Stream.empty
      } else if (resolvedConfigs.length > 1) {
        val feedStreams = resolvedConfigs.map { resolvedConfig =>
          val feedStream = shell.mergeCommandsAndInlineInput(
            resolvedConfig.commands,
            resolvedConfig.inlineInput
          )
          processStreamWithConfig(feedStream, resolvedConfig)
        }
        Stream.emits(feedStreams).parJoin(feedStreams.size)
      } else {
        val resolvedConfig = resolvedConfigs.head
        val inputStream =
          if (
            resolvedConfig.inlineInput.isDefined || resolvedConfig.commands.nonEmpty
          ) {
            shell.mergeCommandsAndInlineInput(
              resolvedConfig.commands,
              resolvedConfig.inlineInput
            )
          } else {
            stdinStream.stdinLinesStream
          }
        processStreamWithConfig(inputStream, resolvedConfig)
      }

      finalStream.intersperse("\n").append(Stream.emit("\n"))
    }
  }

  def processLineWithRef(
    line: String,
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    config: Config
  ): Stream[IO, String] =
    Stream.eval(configYamlRef.get).flatMap { configYaml =>
      processLineWithConfig(line, configYaml, config)
    }

  def processLineWithConfig(
    line: String,
    configYaml: Option[ConfigYaml],
    config: Config
  ): Stream[IO, String] = {
    val resolvedConfigs = ConfigResolver.resolve(config, configYaml)

    if (resolvedConfigs.isEmpty) {
      Stream.empty
    } else if (resolvedConfigs.length > 1) {
      val results = resolvedConfigs.map { resolvedConfig =>
        processLineWithResolvedConfig(line, resolvedConfig)
      }
      Stream.emits(results).parJoinUnbounded
    } else {
      processLineWithResolvedConfig(line, resolvedConfigs.head)
    }
  }

  def processLineWithResolvedConfig(
    line: String,
    resolvedConfig: ResolvedConfig
  ): Stream[IO, String] = {
    val getParser: IO[LogLineParser] =
      if (resolvedConfig.formatIn.contains(FormatIn.Csv)) {
        IO.raiseError(
          new IllegalStateException(
            "CSV format requires header line, cannot process a single line"
          )
        )
      } else {
        IO.pure(makeNonCsvLogLineParser(resolvedConfig))
      }

    Stream.eval(getParser).flatMap { parser =>
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

      Stream
        .emit(line)
        .filter(
          rawFilter(_, resolvedConfig.rawInclude, resolvedConfig.rawExclude)
        )
        .map(parser.parse)
        .filter(logLineFilter.grep)
        .filter(logLineFilter.logLineQueryPredicate)
        .filter(fuzzyFilter.test)
        .through(
          timestampFilter.filterTimestampAfter(resolvedConfig.timestampAfter)
        )
        .through(
          timestampFilter.filterTimestampBefore(
            resolvedConfig.timestampBefore
          )
        )
        .map(formatWithSafety(_, outputLineFormatter))
    }
  }

  private def createCsvProcessStream(
    resolvedConfig: ResolvedConfig,
    lines: Stream[IO, String]
  ): Stream[IO, String] =
    lines.pull.uncons1.flatMap {
      case Some((headerLine, rest)) =>
        val csvHeaderParser = CsvLogLineParser(resolvedConfig, headerLine)

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

        rest
          .filter(
            rawFilter(_, resolvedConfig.rawInclude, resolvedConfig.rawExclude)
          )
          .map(csvHeaderParser.parse)
          .filter(logLineFilter.grep)
          .filter(logLineFilter.logLineQueryPredicate)
          .filter(fuzzyFilter.test)
          .through(
            timestampFilter.filterTimestampAfter(resolvedConfig.timestampAfter)
          )
          .through(
            timestampFilter.filterTimestampBefore(
              resolvedConfig.timestampBefore
            )
          )
          .map(formatWithSafety(_, outputLineFormatter))
          .pull
          .echo
      case None =>
        Pull.done
    }.stream

  private def formatWithSafety(
    parseResult: ParseResult,
    formatter: OutputLineFormatter
  ): String =
    Try(formatter.formatLine(parseResult)) match {
      case Success(formatted) => formatted.toString
      case Failure(_)         => parseResult.raw
    }

  def makeNonCsvLogLineParser(
    resolvedConfig: ResolvedConfig
  ): LogLineParser = {
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    resolvedConfig.formatIn match {
      case Some(FormatIn.Logfmt) => LogfmtLogLineParser(resolvedConfig)
      case Some(FormatIn.Csv) =>
        throw new IllegalStateException(
          "method makeNonCsvLogLineParser does not support csv"
        )
      case _ => JsonLogLineParser(resolvedConfig, jsonPrefixPostfix)
    }
  }

  def rawFilter(
    str: String,
    include: Option[List[String]],
    exclude: Option[List[String]]
  ): Boolean = {
    val includeRegexes: List[Regex] = include.getOrElse(Nil).map(_.r)
    val excludeRegexes: List[Regex] = exclude.getOrElse(Nil).map(_.r)
    val includeMatches = includeRegexes.isEmpty || includeRegexes.exists(
      _.findFirstIn(str).isDefined
    )
    val excludeMatches = excludeRegexes.forall(_.findFirstIn(str).isEmpty)
    includeMatches && excludeMatches
  }
}
