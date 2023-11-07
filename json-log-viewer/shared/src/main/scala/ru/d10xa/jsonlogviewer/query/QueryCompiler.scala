package ru.d10xa.jsonlogviewer.query

object QueryCompiler:
  def apply(code: String): Either[QueryCompilationError, QueryAST] =
    for
      tokens <- QueryLexer(code)
      ast <- QueryParser(tokens)
    yield ast
