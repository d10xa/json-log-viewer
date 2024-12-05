package ru.d10xa.jsonlogviewer

import fs2.Pipe

import java.time.ZonedDateTime

class TimestampFilter:

  def filterTimestampAfter[F[_]](
    t: Option[ZonedDateTime]
  ): Pipe[F, ParseResult, ParseResult] = filterTimestamp(t, _.isAfter(_))

  def filterTimestampBefore[F[_]](
    t: Option[ZonedDateTime]
  ): Pipe[F, ParseResult, ParseResult] = filterTimestamp(t, _.isBefore(_))

  def filterTimestamp[F[_]](
    t: Option[ZonedDateTime],
    predicate: (ZonedDateTime, ZonedDateTime) => Boolean
  ): Pipe[F, ParseResult, ParseResult] =
    p =>
      t match
        case Some(valueFromRequest) =>
          p.filter(p0 => {
            p0.parsed
              .flatMap(_.timestampAsZonedDateTime)
              .forall(l => predicate(l, valueFromRequest))
          })
        case None => p
