package ru.d10xa.jsonlogviewer.decline.yaml

case class ConfigYaml(
  feeds: Option[List[Feed]]
)

object ConfigYaml:
  val empty: ConfigYaml = ConfigYaml(None)
