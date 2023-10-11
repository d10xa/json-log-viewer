package ru.d10xa.jsonlogviewer

import scala.util.matching.Regex
import Config.ConfigGrep

final case class Config(timestamp: TimestampConfig, grep: List[ConfigGrep])

object Config:
  final case class ConfigGrep(key: String, value: Regex)
  
end Config

