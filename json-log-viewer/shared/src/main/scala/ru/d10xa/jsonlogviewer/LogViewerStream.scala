package ru.d10xa.jsonlogviewer

import cats.effect.{IO, Ref}
import fs2.*
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.yaml.{ConfigYaml, Feed}
import ru.d10xa.jsonlogviewer.formatout.{ColorLineFormatter, RawFormatter}
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser
import ru.d10xa.jsonlogviewer.query.QueryAST
import ru.d10xa.jsonlogviewer.shell.ShellImpl

object LogViewerStream {

  private val stdinLinesStream: Stream[IO, String] =
    new StdInLinesStreamImpl().stdinLinesStream

  def stream(
    config: Config,
    configYamlRef: Ref[IO, Option[ConfigYaml]]
  ): Stream[IO, String] = {
    Stream.eval(configYamlRef.get).flatMap { configYamlOpt =>
      val feedsOpt: Option[List[Feed]] =
        configYamlOpt.flatMap(_.feeds).filter(_.nonEmpty)

      val finalStream = feedsOpt match {
        case Some(feeds) =>
          val feedStreams = feeds.map { feed =>
            val feedStream: Stream[IO, String] =
              commandsAndInlineInputToStream(feed.commands, feed.inlineInput)
            processStream(
              config,
              feedStream,
              feed.filter,
              feed.formatIn,
              feed.name
            )
          }
          Stream.emits(feedStreams).parJoin(feedStreams.size)
        case None =>
          processStream(config, stdinLinesStream, None, None, None)
      }

      finalStream
        .intersperse("\n")
        .append(Stream.emit("\n"))
    }
  }

  private def commandsAndInlineInputToStream(
    commands: List[String],
    inlineInput: Option[String]
  ): Stream[IO, String] = {
    new ShellImpl().mergeCommandsAndInlineInput(commands, inlineInput)
  }

  private def processStream(
    baseConfig: Config,
    lines: Stream[IO, String],
    feedFilter: Option[QueryAST],
    feedFormatIn: Option[FormatIn],
    feedName: Option[String]
  ): Stream[IO, String] = {
    val effectiveFormatIn = feedFormatIn.orElse(baseConfig.formatIn)
    val effectiveFilter = feedFilter.orElse(baseConfig.filter)
    val effectiveConfig = baseConfig.copy(
      filter = effectiveFilter,
      formatIn = effectiveFormatIn
    )

    val timestampFilter = TimestampFilter()
    val parseResultKeys = ParseResultKeys(effectiveConfig)
    val logLineFilter = LogLineFilter(effectiveConfig, parseResultKeys)
    val logLineParser = makeLogLineParser(effectiveConfig, effectiveFormatIn)
    val outputLineFormatter = effectiveConfig.formatOut match
      case Some(Config.FormatOut.Raw) => RawFormatter()
      case Some(Config.FormatOut.Pretty) | None =>
        ColorLineFormatter(effectiveConfig, feedName)

    lines
      .map { it =>
        logLineParser.parse(it)
      }
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
      .map(outputLineFormatter.formatLine)
      .map(_.toString)
  }

  private def makeLogLineParser(
    config: Config,
    optFormatIn: Option[FormatIn]
  ): LogLineParser = {
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    optFormatIn match {
      case Some(FormatIn.Logfmt) => LogfmtLogLineParser(config)
      case _                     => JsonLogLineParser(config, jsonPrefixPostfix)
    }
  }
}
