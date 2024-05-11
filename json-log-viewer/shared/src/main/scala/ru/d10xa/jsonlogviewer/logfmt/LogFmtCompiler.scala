package ru.d10xa.jsonlogviewer.logfmt

object LogFmtCompiler:
  def apply(code: String): Either[TokenCompilationError, LogFmtAst] =
    for
      tokens <- LogFmtLexer(code)
      ast <- LogFmtTokenParser(tokens)
    yield ast
