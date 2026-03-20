package ru.d10xa.jsonlogviewer

import cats.effect.IO

class DiagnosticLogImpl extends DiagnosticLog:
  override def error(message: String): IO[Unit] = IO.unit
  override def debug(message: String): IO[Unit] = IO.unit
