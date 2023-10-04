package ru.d10xa.jsonlogviewer

final case class ParseResult(
  raw: String,
  parsed: Option[ParsedLine],
  middle: String,
  prefix: Option[String],
  postfix: Option[String]
)
