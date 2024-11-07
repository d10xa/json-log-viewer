package ru.d10xa.jsonlogviewer.query

import scala.util.parsing.combinator.PackratParsers

object QueryParser extends PackratParsers:
  override type Elem = QueryToken
  private def literal0: Parser[SqlExpr] =
    accept(
      "literal",
      { case lit @ LITERAL(name) =>
        StrLiteral(name)
      }
    )

  private def identifier0: Parser[SqlExpr] =
    accept(
      "identifier",
      { case i @ IDENTIFIER(name) =>
        StrIdentifier(name)
      }
    )

  def statement: Parser[SqlExpr] = expr

  def logicalExpr: Parser[SqlExpr] =
    (compExpr | LPAREN ~> logicalExpr <~ RPAREN) ~ rep(
      (AND | OR) ~ (compExpr | LPAREN ~> logicalExpr <~ RPAREN)
    ) ^^ { case left ~ list =>
      list.foldLeft(left) { case (acc, op ~ right) =>
        op.logicalExpr(acc, right)
      }
    }

  def expr: Parser[SqlExpr] =
    logicalExpr

  def compExpr: Parser[SqlExpr] =
    atomExpr ~ (EQUAL | NOT_EQUAL | NOTLIKE | LIKE) ~ atomExpr ^^ {
      case left ~ op ~ right =>
        op.compareOp(left, right)
    }

  def atomExpr: QueryParser.Parser[SqlExpr] =
    literal0 | identifier0 | (LPAREN ~> expr <~ RPAREN)

  def program: Parser[QueryAST] = phrase(statement) ^^ { stmt =>
    stmt
  }

  def apply(tokens: Seq[QueryToken]): Either[QueryParserError, QueryAST] =
    val reader = new PackratReader(new QueryTokenReader(tokens))
    program(reader) match {
      case Success(result, next) =>
        if (!next.atEnd) {
          Left(QueryParserError(s"Unexpected token: ${next.first}"))
        } else {
          Right(result)
        }
      case NoSuccess(msg, next) =>
        Left(QueryParserError(msg))
      case Failure(msg, _) =>
        Left(QueryParserError(msg))
      case Error(msg, _) =>
        Left(QueryParserError(msg))
    }
