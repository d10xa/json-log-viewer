package ru.d10xa.jsonlogviewer.logfmt

import ru.d10xa.jsonlogviewer.config.ResolvedConfig
import ru.d10xa.jsonlogviewer.LogLineParser
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParsedLine

class LogfmtLogLineParser(config: ResolvedConfig) extends LogLineParser {

  val timestampFieldName: String = config.fieldNames.timestampFieldName
  val levelFieldName: String = config.fieldNames.levelFieldName
  val messageFieldName: String = config.fieldNames.messageFieldName
  val stackTraceFieldName: String = config.fieldNames.stackTraceFieldName
  val loggerNameFieldName: String = config.fieldNames.loggerNameFieldName
  val threadNameFieldName: String = config.fieldNames.threadNameFieldName

  val knownFieldNames: Seq[String] = Seq(
    timestampFieldName,
    levelFieldName,
    stackTraceFieldName,
    loggerNameFieldName,
    threadNameFieldName
  )

  override def parse(s: String): ParseResult =
    val (res: Map[String, String], other: String) = LogFmtCompiler(s) match
      case Left(_) =>
        (Map.empty[String, String], s)
      case Right(value) =>
        LogfmtLogLineParser.toMap(value)
    ParseResult(
      raw = s,
      parsed = Some(
        ParsedLine(
          timestamp = res.get(timestampFieldName),
          level = res.get(levelFieldName),
          message = if other.nonEmpty then Some(other) else None,
          stackTrace = res.get(stackTraceFieldName),
          loggerName = res.get(loggerNameFieldName),
          threadName = res.get(threadNameFieldName),
          otherAttributes = res.--(knownFieldNames)
        )
      ),
      middle = "",
      prefix = None,
      postfix = None
    )
}

object LogfmtLogLineParser:

  def toMap(ast: LogFmtAst): (Map[String, String], String) =
    ast match
      case StatementsAst(asts) =>
        val pairs: Map[String, String] =
          asts.collect {
            case p @ PairAst(key, q: QuotedValueAst) => (key, q.unquoted)
            case p @ PairAst(key, value)             => (key, value.toString)
          }.toMap
        val other = asts.filter {
          case p @ PairAst(key, value) => false
          case _                       => true
        }
        (pairs, other.map(_.toString).mkString)
      case other => (Map.empty, other.toString)

end LogfmtLogLineParser
