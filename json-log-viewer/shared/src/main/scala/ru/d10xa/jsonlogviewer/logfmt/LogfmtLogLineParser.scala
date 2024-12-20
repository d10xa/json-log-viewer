package ru.d10xa.jsonlogviewer.logfmt

import ru.d10xa.jsonlogviewer.HardcodedFieldNames.*
import ru.d10xa.jsonlogviewer.LogLineParser
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParsedLine
import ru.d10xa.jsonlogviewer.decline.Config

class LogfmtLogLineParser(config: Config) extends LogLineParser {

  val timestampFieldName: String = config.timestamp.fieldName

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
          timestamp = res.get(config.timestamp.fieldName),
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

  // TODO rename
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
