package ru.d10xa.jsonlogviewer

import scala.util.matching.Regex
import Config.ConfigGrep
import ru.d10xa.jsonlogviewer.query.QueryAST

final case class Config(timestamp: TimestampConfig, grep: List[ConfigGrep], filter: Option[QueryAST])

object Config:
  final case class ConfigGrep(key: String, value: Regex)
  
end Config
