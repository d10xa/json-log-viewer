package ru.d10xa.jsonlogviewer

import cats.effect.Async
import cats.syntax.all.*
import fs2.*
import fs2.io.*
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

  private def commandsToStream[F[_]: Async](
    commands: List[String]
  ): Stream[F, String] = {
    new ShellImpl[F]().mergeCommands(commands)
  }

  private def stdinLinesStream[F[_]: Async]: Stream[F, String] =
    stdinUtf8[F](1024 * 1024 * 10)
      .repartition(s => Chunk.array(s.split("\n", -1)))
      .filter(_.nonEmpty)

  private def processStream[F[_]: Async](
    baseConfig: Config,
    lines: Stream[F, String],
    feedFilter: Option[QueryAST],
    feedFormatIn: Option[FormatIn],
    feedName: Option[String]
  ): Stream[F, String] = {
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
      .map(logLineParser.parse)
      .filter(logLineFilter.grep)
      .filter(logLineFilter.logLineQueryPredicate)
      .through(
        timestampFilter.filterTimestampAfter[F](effectiveConfig.timestamp.after)
      )
      .through(
        timestampFilter.filterTimestampBefore[F](
          effectiveConfig.timestamp.before
        )
      )
      .map(outputLineFormatter.formatLine)
      .map(_.toString)
  }

  def stream[F[_]: Async](config: Config): Stream[F, String] = {
    val topCommandsOpt: Option[List[String]] =
      config.configYaml.flatMap(_.commands).filter(_.nonEmpty)
    val feedsOpt: Option[List[Feed]] =
      config.configYaml.flatMap(_.feeds).filter(_.nonEmpty)

    val finalStream = feedsOpt match {
      case Some(feeds) =>
        val feedStreams = feeds.map { feed =>
          val feedStream = commandsToStream[F](feed.commands)
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
          case Some(cmds) => commandsToStream[F](cmds)
          case None       => stdinLinesStream[F]
        }
        processStream(config, baseStream, None, None, None)
    }

    finalStream
      .intersperse("\n")
      .append(Stream.emit("\n"))
  }
}
