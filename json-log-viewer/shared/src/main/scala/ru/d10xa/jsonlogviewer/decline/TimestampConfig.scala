package ru.d10xa.jsonlogviewer.decline

import java.time.ZonedDateTime

final case class TimestampConfig(
  fieldName: String, // TODO move to FieldNames config
  after: Option[ZonedDateTime],
  before: Option[ZonedDateTime]
)
