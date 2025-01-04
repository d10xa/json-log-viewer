package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.syntax.all.*
import fs2.*
import fs2.io.*
import ru.d10xa.jsonlogviewer.StdInLinesStreamImpl
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.ConfigInit
import ru.d10xa.jsonlogviewer.decline.ConfigInitImpl
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.formatout.ColorLineFormatter
import ru.d10xa.jsonlogviewer.formatout.RawFormatter
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser
import ru.d10xa.jsonlogviewer.query.QueryAST
import ru.d10xa.jsonlogviewer.shell.ShellImpl

object LogViewerStream {

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

  private def commandsAndInlineInputToStream(
    commands: List[String],
    inlineInput: Option[String]
  ): Stream[IO, String] = {
    new ShellImpl().mergeCommandsAndInlineInput(commands, inlineInput)
  }

  private val stdinLinesStream: Stream[IO, String] =
    new StdInLinesStreamImpl().stdinLinesStream

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

  def stream(config: Config): Stream[IO, String] = {
    val topCommandsOpt: Option[List[String]] =
      config.configYaml.flatMap(_.commands).filter(_.nonEmpty)

    val feedsOpt: Option[List[Feed]] =
      config.configYaml.flatMap(_.feeds).filter(_.nonEmpty)

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
            feed.name.some
          )
        }
        Stream.emits(feedStreams).parJoin(feedStreams.size)

      case None =>
        val baseStream = topCommandsOpt match {
          case Some(cmds) =>
            commandsAndInlineInputToStream(cmds, None)
          case None       =>
            stdinLinesStream
        }
        processStream(config, baseStream, None, None, None)
    }

    finalStream
      .intersperse("\n")
      .append(Stream.emit("\n"))
  }
}
