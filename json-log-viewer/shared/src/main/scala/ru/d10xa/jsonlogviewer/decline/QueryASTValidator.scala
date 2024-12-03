package ru.d10xa.jsonlogviewer.decline

import cats.data.Validated
import cats.data.ValidatedNel
import ru.d10xa.jsonlogviewer.query.QueryAST
import ru.d10xa.jsonlogviewer.query.QueryCompiler

object QueryASTValidator {
  def toValidatedQueryAST(str: String): ValidatedNel[String, QueryAST] =
    QueryCompiler(str) match
      case Left(value) => Validated.invalidNel(value.toString)
      case Right(value) => Validated.validNel(value)

}
