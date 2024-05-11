package ru.d10xa.jsonlogviewer.logfmt

trait TokenCompilationError

case class TokenLexerError(msg: String) extends TokenCompilationError

case class TokenParserError(msg: String) extends TokenCompilationError

