package ru.d10xa.jsonlogviewer

import cats.effect.IO

class DiagnosticLogImpl(debugEnabled: Boolean) extends DiagnosticLog:

  private def stderrPrint(s: String): IO[Unit] =
    fs2.Stream
      .emit(s)
      .through(fs2.text.utf8.encode)
      .through(fs2.io.stderr[IO])
      .compile
      .drain

  override def error(message: String): IO[Unit] =
    stderrPrint(
      s"\n${fansi.Color.Red(s"[json-log-viewer:ERROR] $message").render}\n"
    )

  override def debug(message: String): IO[Unit] =
    if (debugEnabled)
      stderrPrint(
        s"${fansi.Color.Yellow(s"[json-log-viewer:DEBUG] $message").render}\n"
      )
    else IO.unit
