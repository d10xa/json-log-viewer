package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.Config.ConfigGrep
import HardcodedFieldNames.messageFieldName
import HardcodedFieldNames.levelFieldName
import HardcodedFieldNames.loggerNameFieldName
import HardcodedFieldNames.threadNameFieldName
import HardcodedFieldNames.stackTraceFieldName

class LogLineFilter(config: Config) {
  def grep(
    parseResult: ParseResult
  ): Boolean =
    config.grep
      .flatMap { case ConfigGrep(grepKey, regex) =>
        getByKey(grepKey, parseResult).map(regex.matches)
      } match
      case Nil  => true
      case list => list.reduce(_ || _)

  def getByKey(
    fieldName: String,
    parseResult: ParseResult
  ): Option[String] = fieldName match {
    case config.timestamp.fieldName => parseResult.parsed.map(_.timestamp)
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
