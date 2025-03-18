package ru.d10xa.jsonlogviewer.csv

import ru.d10xa.jsonlogviewer.LogLineParser
import ru.d10xa.jsonlogviewer.ParseResult
import ru.d10xa.jsonlogviewer.ParsedLine
import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.HardcodedFieldNames.*

class CsvLogLineParser(config: Config, headers: List[String]) extends LogLineParser {
  private val csvParser = new CsvParser()
  private val timestampFieldName: String = config.timestamp.fieldName

  private val knownFieldNames: Seq[String] = Seq(
    timestampFieldName,
    levelFieldName,
    messageFieldName,
    stackTraceFieldName,
    loggerNameFieldName,
    threadNameFieldName
  )

  private val headerIndices: Map[String, Int] = headers.zipWithIndex.toMap

  override def parse(s: String): ParseResult = {
    val values = csvParser.parseLine(s)

    val fieldsMap = headerIndices.flatMap { case (header, index) =>
      if (index < values.size) Some(header -> values(index))
      else None
    }

    val timestamp = fieldsMap.get(timestampFieldName)
    val level = fieldsMap.get(levelFieldName)
    val message = fieldsMap.get(messageFieldName)
    val stackTrace = fieldsMap.get(stackTraceFieldName)
    val loggerName = fieldsMap.get(loggerNameFieldName)
    val threadName = fieldsMap.get(threadNameFieldName)

    val otherAttributes = fieldsMap.view.filterKeys(!knownFieldNames.contains(_)).toMap

    ParseResult(
      raw = s,
      parsed = Some(
        ParsedLine(
          timestamp = timestamp,
          level = level,
          message = message,
          stackTrace = stackTrace,
          loggerName = loggerName,
          threadName = threadName,
          otherAttributes = otherAttributes
        )
      ),
      middle = "",
      prefix = None,
      postfix = None
    )
  }
}

object CsvLogLineParser {
  def apply(config: Config, headerLine: String): CsvLogLineParser = {
    val csvParser = new CsvParser()
    val headers = csvParser.parseLine(headerLine)
    new CsvLogLineParser(config, headers)
  }
}
