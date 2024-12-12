package ru.d10xa.jsonlogviewer.decline.yaml

import ru.d10xa.jsonlogviewer.decline.Config
import ru.d10xa.jsonlogviewer.decline.yaml.ConfigYaml
import ru.d10xa.jsonlogviewer.query.QueryAST

case class ConfigYaml(
  filter: Option[QueryAST],
  formatIn: Option[Config.FormatIn],
  commands: Option[List[String]],
  feeds: Option[List[Feed]]
)

object ConfigYaml:
  val empty: ConfigYaml = ConfigYaml(None, None, None, None)

