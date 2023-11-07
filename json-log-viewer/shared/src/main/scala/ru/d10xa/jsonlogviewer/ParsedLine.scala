package ru.d10xa.jsonlogviewer

import java.time.ZonedDateTime
import scala.util.Try

final case class ParsedLine(
    timestamp: Option[String],
    level: Option[String],
    message: Option[String],
    stackTrace: Option[String],
    loggerName: Option[String],
    threadName: Option[String],
    otherAttributes: Map[String, String]
):
  val timestampAsZonedDateTime: Option[ZonedDateTime] = timestamp.flatMap(t => Try(
    ZonedDateTime.parse(t)
  ).toOption)
