package ru.d10xa.jsonlogviewer.logfmt

import ru.d10xa.jsonlogviewer.Config
import ru.d10xa.jsonlogviewer.HardcodedFieldNames.*
import ru.d10xa.jsonlogviewer.LogLineParser
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParsedLine

class LogfmtLogLineParser(config: Config)
  extends LogLineParser {

  val timestampFieldName: String = config.timestamp.fieldName

  val knownFieldNames: Seq[String] = Seq(
    timestampFieldName,
    levelFieldName,
    stackTraceFieldName,
    loggerNameFieldName,
    threadNameFieldName
  )

  override def parse(s: String): ParseResult =
    val (res: Map[String, String], other: Seq[String]) = LogFmtDecoder.decode(s)
    ParseResult(
      raw = s,
      parsed = Some(
        ParsedLine(
          timestamp = res.get(config.timestamp.fieldName),
          level = res.get(levelFieldName),
          message = if other.nonEmpty then Some(other.mkString(" ")) else None,
          stackTrace = res.get(stackTraceFieldName),
          loggerName = res.get(loggerNameFieldName),
          threadName = res.get(threadNameFieldName),
          otherAttributes = res.--(knownFieldNames)
        )
      ),
      middle = "", prefix = None, postfix = None
    )

}
