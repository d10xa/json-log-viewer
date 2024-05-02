package ru.d10xa.jsonlogviewer.logfmt

import scala.util.parsing.input.NoPosition
import scala.util.parsing.input.Position
import scala.util.parsing.input.Reader

class LogFmtTokenReader(tokens: Seq[LogFmtToken]) extends Reader[LogFmtToken]:
  override def first: LogFmtToken = tokens.head

  override def atEnd: Boolean = tokens.isEmpty

  override def pos: Position = NoPosition

  override def rest: Reader[LogFmtToken] = new LogFmtTokenReader(tokens.tail)
