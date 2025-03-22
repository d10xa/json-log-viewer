package ru.d10xa.jsonlogviewer.decline.yaml

case class FieldNames(
  timestamp: Option[String],
  level: Option[String],
  message: Option[String],
  stackTrace: Option[String],
  loggerName: Option[String],
  threadName: Option[String]
)
