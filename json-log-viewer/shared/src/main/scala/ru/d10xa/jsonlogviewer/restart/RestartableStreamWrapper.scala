package ru.d10xa.jsonlogviewer.restart

import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all.*
import fs2.Stream
import java.time.ZonedDateTime
import ru.d10xa.jsonlogviewer.ParseResult

object RestartableStreamWrapper {

  def wrapWithTimestampTracking(
    stream: Stream[IO, ParseResult],
    lastTsRef: Ref[IO, Option[ZonedDateTime]],
    isRestartRef: Ref[IO, Boolean]
  ): Stream[IO, ParseResult] =
    stream
      .evalFilter { pr =>
        for {
          isRestart <- isRestartRef.get
          lastTs <- lastTsRef.get
        } yield
          if (!isRestart) {
            // Not a restart, pass through all messages
            true
          } else {
            // After restart, filter messages with timestamp <= lastTimestamp
            val prTimestamp = pr.parsed.flatMap(_.timestampAsZonedDateTime)
            lastTs match {
              case None =>
                // No previous timestamp recorded, pass through
                true
              case Some(last) =>
                prTimestamp match {
                  case None =>
                    // Message has no timestamp, pass through (can't deduplicate)
                    true
                  case Some(ts) =>
                    // Only pass through if timestamp is strictly after the last seen
                    ts.isAfter(last)
                }
            }
          }
      }
      .evalTap { pr =>
        // Update the last timestamp after emitting each message
        pr.parsed.flatMap(_.timestampAsZonedDateTime) match {
          case Some(ts) =>
            lastTsRef.get.flatMap { lastOpt =>
              val isAfterLast = lastOpt.forall(last => ts.isAfter(last))
              lastTsRef.update {
                case None       => Some(ts)
                case Some(last) => Some(if (ts.isAfter(last)) ts else last)
              } >> (if (isAfterLast) isRestartRef.set(false) else IO.unit)
            }
          case None =>
            // No timestamp in this message, don't clear restart flag
            IO.unit
        }
      }

  def createOnRestartCallback(isRestartRef: Ref[IO, Boolean]): IO[Unit] =
    isRestartRef.set(true)
}
