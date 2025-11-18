package ru.d10xa.jsonlogviewer.decline.yaml

import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.query.QueryAST

case class Feed(
  name: Option[String],
  commands: List[String], // TODO option
  inlineInput: Option[String],
  filter: Option[QueryAST],
  formatIn: Option[FormatIn],
  fieldNames: Option[FieldNames],
  rawInclude: Option[List[String]],
  rawExclude: Option[List[String]],
  fuzzyInclude: Option[List[String]],
  fuzzyExclude: Option[List[String]],
  excludeFields: Option[List[String]],
  showEmptyFields: Option[Boolean]
)
