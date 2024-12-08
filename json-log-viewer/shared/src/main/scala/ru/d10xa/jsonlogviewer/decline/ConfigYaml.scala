package ru.d10xa.jsonlogviewer.decline

import ru.d10xa.jsonlogviewer.query.QueryAST

case class ConfigYaml(
  filter: Option[QueryAST],
  formatIn: Option[Config.FormatIn],
  commands: Option[List[String]]
)

object ConfigYaml:
  val empty: ConfigYaml = ConfigYaml(None, None, None)
