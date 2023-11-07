package ru.d10xa.jsonlogviewer.query

import scala.util.parsing.input.NoPosition
import scala.util.parsing.input.Position
import scala.util.parsing.input.Reader

class QueryTokenReader(tokens: Seq[QueryToken]) extends Reader[QueryToken]:
  override def first: QueryToken = tokens.head

  override def atEnd: Boolean = tokens.isEmpty

  override def pos: Position = NoPosition

  override def rest: Reader[QueryToken] = new QueryTokenReader(tokens.tail)
