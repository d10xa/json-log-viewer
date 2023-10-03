package ru.d10xa.jsonlogviewer

final case class ParseResult(raw: String, parsed: Option[ParsedLine])
