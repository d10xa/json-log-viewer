package ru.d10xa.jsonlogviewer

import io.circe.Json
import io.circe.Decoder
import io.circe.HCursor
import io.circe.parser.*
import cats.syntax.all.*

class LogLineParser(config: Config) {
  given Decoder[ParsedLine] = (c: HCursor) =>
    val timestampFieldName = config.timestamp.fieldName
    val levelFieldName = "level"
    val messageFieldName = "message"
    val stackTraceFieldName = "stack_trace"
    val loggerNameFieldName = "logger_name"
    val threadNameFieldName = "thread_name"
    val knownFieldNames = Seq(
      timestampFieldName,
      levelFieldName,
      messageFieldName,
      stackTraceFieldName,
      loggerNameFieldName,
      threadNameFieldName)
    for
      timestamp <- c.downField(timestampFieldName).as[String]
      levelOpt <- c.downField(levelFieldName).as[Option[String]]
      messageOpt <- c.downField(messageFieldName).as[Option[String]]
      stackTraceOpt <- c.downField(stackTraceFieldName).as[Option[String]]
      loggerNameOpt <- c.downField(loggerNameFieldName).as[Option[String]]
      threadNameOpt <- c.downField(threadNameFieldName).as[Option[String]]
      attributes <- c.as[Map[String, Json]]
        .map(_.view.mapValues(_.toString).toMap.--(knownFieldNames)
      )
    yield ParsedLine(
      timestamp = timestamp,
      message = messageOpt,
      stackTrace = stackTraceOpt,
      level = levelOpt,
      threadName = threadNameOpt,
      loggerName = loggerNameOpt,
      otherAttributes = attributes
    )
  def parse(s: String): ParseResult = decode[ParsedLine](s).toOption
    .map(pl => ParseResult(raw = s, parsed = pl.some))
    .getOrElse(ParseResult(raw = s, None))
}
