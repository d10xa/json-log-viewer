package ru.d10xa.jsonlogviewer

import cats.syntax.all.*
import io.circe.parser.*
import io.circe.Decoder
import io.circe.HCursor
import io.circe.Json
import ru.d10xa.jsonlogviewer.config.ResolvedConfig

class JsonLogLineParser(
  config: ResolvedConfig,
  jsonPrefixPostfix: JsonPrefixPostfix
) extends LogLineParser:
  given Decoder[ParsedLine] = (c: HCursor) =>
    val timestampFieldName = config.fieldNames.timestampFieldName
    val levelFieldName = config.fieldNames.levelFieldName
    val messageFieldName = config.fieldNames.messageFieldName
    val stackTraceFieldName = config.fieldNames.stackTraceFieldName
    val loggerNameFieldName = config.fieldNames.loggerNameFieldName
    val threadNameFieldName = config.fieldNames.threadNameFieldName

    val knownFieldNames = Set(
      timestampFieldName,
      "@timestamp",
      levelFieldName,
      "level",
      messageFieldName,
      "message",
      stackTraceFieldName,
      "stack_trace",
      loggerNameFieldName,
      "logger_name",
      threadNameFieldName,
      "thread_name"
    )

    // Function to find value by multiple possible keys
    def findByKeys(keys: String*): Option[String] =
      keys.flatMap { key =>
        c.downField(key).as[Option[String]].getOrElse(None)
      }.headOption

    def mapOtherAttributes(m: Map[String, Json]): Map[String, String] =
      m.view
        .mapValues { v =>
          v.fold(
            jsonNull = "null",
            jsonBoolean = _.booleanValue().toString,
            jsonNumber = _.toString,
            jsonString = identity,
            jsonArray = _.toString,
            jsonObject = _.toString
          )
        }
        .toMap
        .filter { case (k, _) => !knownFieldNames.contains(k) }

    for
      // Check both standard and configured field names
      timestampOpt <- Either.right(
        findByKeys("@timestamp", "timestamp", timestampFieldName)
      )
      levelOpt <- Either.right(findByKeys("level", levelFieldName))
      messageOpt <- Either.right(findByKeys("message", messageFieldName))
      stackTraceOpt <- Either.right(
        findByKeys("stack_trace", stackTraceFieldName)
      )
      loggerNameOpt <- Either.right(
        findByKeys("logger_name", loggerNameFieldName)
      )
      threadNameOpt <- Either.right(
        findByKeys("thread_name", threadNameFieldName)
      )
      attributes <- c
        .as[Map[String, Json]]
        .map(mapOtherAttributes)
    yield ParsedLine(
      timestamp = timestampOpt,
      message = messageOpt,
      stackTrace = stackTraceOpt,
      level = levelOpt,
      threadName = threadNameOpt,
      loggerName = loggerNameOpt,
      otherAttributes = attributes
    )
  override def parse(s: String): ParseResult =
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
