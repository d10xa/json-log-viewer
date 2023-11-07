package ru.d10xa.jsonlogviewer

import io.circe.Json
import io.circe.Decoder
import io.circe.HCursor
import io.circe.parser.*
import cats.syntax.all.*

import HardcodedFieldNames._

class LogLineParser(config: Config, jsonPrefixPostfix: JsonPrefixPostfix) {
  given Decoder[ParsedLine] = (c: HCursor) =>
    val timestampFieldName = config.timestamp.fieldName
    
    val knownFieldNames = Seq(
      timestampFieldName,
      levelFieldName,
      messageFieldName,
      stackTraceFieldName,
      loggerNameFieldName,
      threadNameFieldName
    )
    for
      timestampOpt <- c.downField(timestampFieldName).as[Option[String]]
      levelOpt <- c.downField(levelFieldName).as[Option[String]]
      messageOpt <- c.downField(messageFieldName).as[Option[String]]
      stackTraceOpt <- c.downField(stackTraceFieldName).as[Option[String]]
      loggerNameOpt <- c.downField(loggerNameFieldName).as[Option[String]]
      threadNameOpt <- c.downField(threadNameFieldName).as[Option[String]]
      attributes <- c
        .as[Map[String, Json]]
        .map(_.view.mapValues(_.toString).toMap.--(knownFieldNames))
    yield ParsedLine(
      timestamp = timestampOpt,
      message = messageOpt,
      stackTrace = stackTraceOpt,
      level = levelOpt,
      threadName = threadNameOpt,
      loggerName = loggerNameOpt,
      otherAttributes = attributes
    )
  def parse(s: String): ParseResult =
    val (middle, prefixOpt, postfixOpt) = jsonPrefixPostfix.detectJson(s)
    decode[ParsedLine](middle).toOption
      .map(pl =>
        ParseResult(
          raw = s,
          parsed = pl.some,
          middle = middle,
          prefix = prefixOpt,
          postfix = postfixOpt
        )
      )
      .getOrElse(
        ParseResult(
          raw = s,
          parsed = None,
          middle = middle,
          prefix = prefixOpt,
          postfix = postfixOpt
        )
      )

}
