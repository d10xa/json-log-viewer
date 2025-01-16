package ru.d10xa.jsonlogviewer.decline.yaml

import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.query.QueryAST

case class Feed(
  name: Option[String],
  commands: List[String],
  inlineInput: Option[String],
  filter: Option[QueryAST],
  formatIn: Option[FormatIn],
  rawInclude: Option[List[String]],
  rawExclude: Option[List[String]]
)
