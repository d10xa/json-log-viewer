package ru.d10xa.jsonlogviewer

import cats.effect.Async
import fs2.*
import fs2.io.*
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.decline.ConfigInit
import ru.d10xa.jsonlogviewer.decline.ConfigInitImpl
import ru.d10xa.jsonlogviewer.decline.DeclineOpts
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.formatout.ColorLineFormatter
import ru.d10xa.jsonlogviewer.formatout.RawFormatter
import ru.d10xa.jsonlogviewer.logfmt.LogfmtLogLineParser
import ru.d10xa.jsonlogviewer.shell.ShellImpl
object LogViewerStream {

  def stream[F[_]: Async](
    config: Config
  ): Stream[F, String] = {
    val stdinLinesStream: Stream[F, String] =
      stdinUtf8[F](1024 * 1024 * 10)
        .repartition(s => Chunk.array(s.split("\n", -1)))
        .filter(_.nonEmpty)
    val timestampFilter = TimestampFilter()
    val jsonPrefixPostfix = JsonPrefixPostfix(JsonDetector())
    val outputLineFormatter = config.formatOut match
      case Some(Config.FormatOut.Raw)           => RawFormatter()
      case Some(Config.FormatOut.Pretty) | None => ColorLineFormatter(config)

    val parseResultKeys = ParseResultKeys(config)
    val logLineFilter = LogLineFilter(config, parseResultKeys)
    val logLineParser = config.formatIn match {
      case Some(FormatIn.Logfmt) => LogfmtLogLineParser(config)
      case _                     => JsonLogLineParser(config, jsonPrefixPostfix)
    }
    val commandsOpt = config.configYaml.flatMap(_.commands).filter(_.nonEmpty)
    val stream: Stream[F, String] = commandsOpt match {
      case Some(cmds) if cmds.nonEmpty =>
        new ShellImpl[F]().mergeCommands(cmds)
      case _ =>
        stdinLinesStream
    }
    val s1: Stream[F, ParseResult] = stream
      .map(logLineParser.parse)
      .filter(logLineFilter.grep)
      .filter(logLineFilter.logLineQueryPredicate)

    val p: Pipe[F, ParseResult, ParseResult] = timestampFilter.filterTimestampAfter[F](config.timestamp.after)
    val s2 = s1.through(p)
    s2
      .through(timestampFilter.filterTimestampBefore[F](config.timestamp.before))
      .map(outputLineFormatter.formatLine)
      .map(_.toString)
      .intersperse("\n")
      .append(Stream.emit("\n"))
  }
}
