package ru.d10xa.jsonlogviewer.decline.yaml

case class ConfigYaml(
  fieldNames: Option[FieldNames],
  feeds: Option[List[Feed]]
)

object ConfigYaml:
  val empty: ConfigYaml = ConfigYaml(None, None)
