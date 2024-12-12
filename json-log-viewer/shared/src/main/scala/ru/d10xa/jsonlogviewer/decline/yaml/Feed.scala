package ru.d10xa.jsonlogviewer.decline.yaml

import ru.d10xa.jsonlogviewer.decline.Config.FormatIn
import ru.d10xa.jsonlogviewer.query.QueryAST

case class Feed(
  name: String,
  commands: List[String],
  filter: Option[QueryAST],
  formatIn: Option[FormatIn]
)
