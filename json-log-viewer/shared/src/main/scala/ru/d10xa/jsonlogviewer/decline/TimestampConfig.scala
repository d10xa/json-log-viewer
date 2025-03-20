package ru.d10xa.jsonlogviewer.decline

import java.time.ZonedDateTime

final case class TimestampConfig(
  after: Option[ZonedDateTime],
  before: Option[ZonedDateTime]
)
