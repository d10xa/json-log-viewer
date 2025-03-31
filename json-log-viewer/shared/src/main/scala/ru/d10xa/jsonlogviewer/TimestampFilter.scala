package ru.d10xa.jsonlogviewer

import cats.effect.IO
import fs2.Pipe
import java.time.ZonedDateTime

class TimestampFilter:

  def filterTimestampAfter(
    t: Option[ZonedDateTime]
  ): Pipe[IO, ParseResult, ParseResult] = filterTimestamp(t, _.isAfter(_))

  def filterTimestampBefore(
    t: Option[ZonedDateTime]
  ): Pipe[IO, ParseResult, ParseResult] = filterTimestamp(t, _.isBefore(_))

  def filterTimestamp(
    t: Option[ZonedDateTime],
    predicate: (ZonedDateTime, ZonedDateTime) => Boolean
  ): Pipe[IO, ParseResult, ParseResult] =
    p =>
      t match
        case Some(valueFromRequest) =>
          p.filter(p0 =>
            p0.parsed
              .flatMap(_.timestampAsZonedDateTime)
              .forall(l => predicate(l, valueFromRequest))
          )
        case None => p
