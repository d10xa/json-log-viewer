package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.Config.ConfigGrep
import ru.d10xa.jsonlogviewer.query.QueryAST

import scala.util.matching.Regex

final case class Config(
  timestamp: TimestampConfig,
  grep: List[ConfigGrep],
  filter: Option[QueryAST],
  formatIn: Config.FormatIn
)

object Config:
  final case class ConfigGrep(key: String, value: Regex)

  enum FormatIn:
    case Json, Logfmt

end Config
