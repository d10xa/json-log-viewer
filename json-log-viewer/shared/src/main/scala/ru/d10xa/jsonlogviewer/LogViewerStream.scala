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

  private def makeLogLineParser(optFormatIn: Option[FormatIn], config: Config): LogLineParser = {
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

  def stream[F[_]: Async](
    config: Config
  ): Stream[F, String] = {
    
    config.configYaml.map(_.feeds)

    val stdinLinesStream: Stream[F, String] =
      stdinUtf8[F](1024 * 1024 * 10)
        .repartition(s => Chunk.array(s.split("\n", -1)))
        .filter(_.nonEmpty)
    val timestampFilter = TimestampFilter()
    val outputLineFormatter = config.formatOut match
      case Some(Config.FormatOut.Raw)           => RawFormatter()
      case Some(Config.FormatOut.Pretty) | None => ColorLineFormatter(config)

    val parseResultKeys = ParseResultKeys(config)
    val logLineFilter = LogLineFilter(config, parseResultKeys)
    val logLineParser = makeLogLineParser(config.formatIn, config)
    val commandsOpt: Option[List[String]] =
      config.configYaml.flatMap(_.commands).filter(_.nonEmpty)
    val stream: Stream[F, String] = commandsOpt match {
      case Some(cmds) if cmds.nonEmpty => commandsToStream[F](cmds)
      case _                           => stdinLinesStream
    }
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
}
