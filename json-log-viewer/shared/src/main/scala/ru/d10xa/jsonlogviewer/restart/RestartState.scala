package ru.d10xa.jsonlogviewer.restart

import cats.effect.IO
import cats.effect.Ref
import java.time.ZonedDateTime

final case class RestartState(
  lastTsRef: Ref[IO, Option[ZonedDateTime]],
  isRestartRef: Ref[IO, Boolean]
)

object RestartState {
  def create: IO[RestartState] =
    for {
      lastTsRef <- Ref.of[IO, Option[ZonedDateTime]](None)
      isRestartRef <- Ref.of[IO, Boolean](false)
    } yield RestartState(lastTsRef, isRestartRef)
}
