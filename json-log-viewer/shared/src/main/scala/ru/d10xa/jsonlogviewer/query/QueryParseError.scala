package ru.d10xa.jsonlogviewer.query

trait QueryCompilationError

case class QueryLexerError(msg: String) extends QueryCompilationError
case class QueryParserError(msg: String) extends QueryCompilationError
