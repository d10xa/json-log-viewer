package ru.d10xa.jsonlogviewer

import HardcodedFieldNames.messageFieldName
import HardcodedFieldNames.levelFieldName
import HardcodedFieldNames.loggerNameFieldName
import HardcodedFieldNames.threadNameFieldName
import HardcodedFieldNames.stackTraceFieldName
import ru.d10xa.jsonlogviewer.decline.Config
class ParseResultKeys(config: Config) {
  def getByKey(
    parseResult: ParseResult,
    fieldName: String
  ): Option[String] = fieldName match {
    case config.timestamp.fieldName => parseResult.parsed.flatMap(_.timestamp)
    case `messageFieldName`         => parseResult.parsed.flatMap(_.message)
    case `levelFieldName`           => parseResult.parsed.flatMap(_.level)
    case `loggerNameFieldName`      => parseResult.parsed.flatMap(_.loggerName)
    case `threadNameFieldName`      => parseResult.parsed.flatMap(_.threadName)
    case `stackTraceFieldName`      => parseResult.parsed.flatMap(_.stackTrace)
    case "prefix"                   => parseResult.prefix
    case "postfix"                  => parseResult.postfix
    case other => parseResult.parsed.flatMap(_.otherAttributes.get(other))
  }
}
