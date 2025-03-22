package ru.d10xa.jsonlogviewer.decline

final case class FieldNamesConfig(
  timestampFieldName: String,
  levelFieldName: String,
  messageFieldName: String,
  stackTraceFieldName: String,
  loggerNameFieldName: String,
  threadNameFieldName: String
)
