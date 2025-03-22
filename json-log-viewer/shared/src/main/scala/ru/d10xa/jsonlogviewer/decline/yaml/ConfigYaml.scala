package ru.d10xa.jsonlogviewer.decline.yaml

case class ConfigYaml(
  fieldNames: Option[FieldNames],
  feeds: Option[List[Feed]],
  showEmptyFields: Option[Boolean]
)

object ConfigYaml:
  val empty: ConfigYaml = ConfigYaml(None, None, None)
