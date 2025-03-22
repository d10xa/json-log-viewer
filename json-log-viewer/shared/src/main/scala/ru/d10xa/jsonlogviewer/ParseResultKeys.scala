package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.config.ResolvedConfig

class ParseResultKeys(config: ResolvedConfig) {

  def getByKey(
    parseResult: ParseResult,
    fieldName: String
  ): Option[String] = {
    import config.fieldNames.*

    // Determine which standard field the fieldName might refer to
    if (
      fieldName == "timestamp" || fieldName == "@timestamp" || fieldName == timestampFieldName
    ) {
      parseResult.parsed.flatMap(_.timestamp)
    } else if (fieldName == "level" || fieldName == levelFieldName) {
      parseResult.parsed.flatMap(_.level)
    } else if (fieldName == "message" || fieldName == messageFieldName) {
      parseResult.parsed.flatMap(_.message)
    } else if (fieldName == "stack_trace" || fieldName == stackTraceFieldName) {
      parseResult.parsed.flatMap(_.stackTrace)
    } else if (fieldName == "logger_name" || fieldName == loggerNameFieldName) {
      parseResult.parsed.flatMap(_.loggerName)
    } else if (fieldName == "thread_name" || fieldName == threadNameFieldName) {
      parseResult.parsed.flatMap(_.threadName)
    } else if (fieldName == "prefix") {
      parseResult.prefix
    } else if (fieldName == "postfix") {
      parseResult.postfix
    } else {
      parseResult.parsed.flatMap(_.otherAttributes.get(fieldName))
    }
  }
}
