package ru.d10xa.jsonlogviewer.decline

import ru.d10xa.jsonlogviewer.decline.Config.ConfigGrep
import ru.d10xa.jsonlogviewer.query.QueryAST
import scala.util.matching.Regex

final case class Config(
  configFile: Option[ConfigFile],
  fieldNames: FieldNamesConfig,
  timestamp: TimestampConfig,
  grep: List[ConfigGrep],
  filter: Option[QueryAST],
  formatIn: Option[Config.FormatIn],
  formatOut: Option[Config.FormatOut],
  showEmptyFields: Boolean,
  commands: List[String],
  restart: Boolean,
  restartDelayMs: Option[Long],
  maxRestarts: Option[Int]
)

object Config:
  final case class ConfigGrep(key: String, value: Regex)

  enum FormatIn:
    case Json, Logfmt, Csv

  enum FormatOut:
    case Pretty, Raw
