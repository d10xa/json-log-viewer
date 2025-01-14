package ru.d10xa.jsonlogviewer

import cats.effect.IO
import cats.effect.Ref
import fs2.*
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.decline.yaml.Feed
import ru.d10xa.jsonlogviewer.formatout.ColorLineFormatter
import ru.d10xa.jsonlogviewer.formatout.RawFormatter
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
          val feedStreams = feeds.zipWithIndex.map { (feed, index) =>
            val feedStream: Stream[IO, String] =
              commandsAndInlineInputToStream(feed.commands, feed.inlineInput)
            processStream(
              config,
              feedStream,
              configYamlRef,
              index
            )
          }
          Stream.emits(feedStreams).parJoin(feedStreams.size)
        case None =>
          processStream(config, stdinLinesStream, configYamlRef, -1)
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

  def makeLogLineParser(
    config: Config,
    optFormatIn: Option[FormatIn]
  ): LogLineParser = {
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    optFormatIn match {
      case Some(FormatIn.Logfmt) => LogfmtLogLineParser(config)
      case _                     => JsonLogLineParser(config, jsonPrefixPostfix)
    }
  }

  private def processStream(
    baseConfig: Config,
    lines: Stream[IO, String],
    configYamlRef: Ref[IO, Option[ConfigYaml]],
    index: Int
  ): Stream[IO, String] = {
    for {
      line <- lines
      optConfigYaml <- Stream.eval(configYamlRef.get)
      formatIn = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.formatIn))
        .orElse(baseConfig.formatIn)
      filter = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.filter))
        .orElse(baseConfig.filter)
      feedName = optConfigYaml
        .flatMap(_.feeds)
        .flatMap(_.lift(index).flatMap(_.name))
      effectiveConfig = baseConfig.copy(
        filter = filter,
        formatIn = formatIn
      )
      timestampFilter = TimestampFilter()
      parseResultKeys = ParseResultKeys(effectiveConfig)
      logLineFilter = LogLineFilter(effectiveConfig, parseResultKeys)
      logLineParser = makeLogLineParser(effectiveConfig, formatIn)
      outputLineFormatter = effectiveConfig.formatOut match
        case Some(Config.FormatOut.Raw) => RawFormatter()
        case Some(Config.FormatOut.Pretty) | None =>
          ColorLineFormatter(effectiveConfig, feedName)

      evaluatedLine <- Stream
        .emit(logLineParser.parse(line))
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
    } yield evaluatedLine

  }

}
