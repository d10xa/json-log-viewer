package ru.d10xa.jsonlogviewer.decline

import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.Config.ConfigGrep
import ru.d10xa.jsonlogviewer.decline.ConfigFile
import ru.d10xa.jsonlogviewer.decline.TimestampConfig
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.query.QueryAST

import scala.util.matching.Regex

final case class Config(
  configFile: Option[ConfigFile],
  configYaml: Option[ConfigYaml],
  timestamp: TimestampConfig,
  grep: List[ConfigGrep],
  filter: Option[QueryAST],
  formatIn: Option[Config.FormatIn],
  formatOut: Option[Config.FormatOut],
)

object Config:
  final case class ConfigGrep(key: String, value: Regex)

  enum FormatIn:
    case Json, Logfmt
    
  enum FormatOut:
    case Pretty, Raw

end Config
