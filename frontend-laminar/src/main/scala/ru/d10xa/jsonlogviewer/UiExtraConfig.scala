package ru.d10xa.jsonlogviewer

import ru.d10xa.jsonlogviewer.decline.yaml.FieldNames

case class UiExtraConfig(
  fuzzyInclude: Option[List[String]],
  fuzzyExclude: Option[List[String]],
  showEmptyFields: Boolean,
  excludeFields: Option[List[String]],
  fieldNames: Option[FieldNames]
)
