package ru.d10xa.jsonlogviewer

import java.time.ZonedDateTime

final case class TimestampConfig(
    fieldName: String,
    after: Option[ZonedDateTime],
    before: Option[ZonedDateTime]
)
