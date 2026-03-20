package ru.d10xa.jsonlogviewer

import cats.effect.IO

trait DiagnosticLog:
  def error(message: String): IO[Unit]
  def debug(message: String): IO[Unit]
