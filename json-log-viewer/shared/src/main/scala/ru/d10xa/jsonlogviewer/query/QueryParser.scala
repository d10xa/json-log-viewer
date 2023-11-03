package ru.d10xa.jsonlogviewer.query

import scala.util.parsing.combinator.Parsers

object QueryParser extends Parsers:
  override type Elem = QueryToken
  private def literal: Parser[LITERAL] = {
    accept("literal", { case lit @ LITERAL(name) => lit })
  }

  private def identifier: Parser[IDENTIFIER] = {
    accept("identifier", { case i @ IDENTIFIER(name) => i })
  }

  def statement: Parser[Binop] = {
    or | eq | neq
  }

  def program: Parser[QueryAST] = phrase(statement)

  def eq: QueryParser.Parser[Eq] = {
    (identifier ~ EQUAL ~ literal) ^^ { case id ~ eq ~ lit =>
      Eq(StrIdentifier(id.str), StrLiteral(lit.str))
    }
  }
  def neq: QueryParser.Parser[Neq] = {
    (identifier ~ NOT_EQUAL ~ literal) ^^ { case id ~ neq ~ lit =>
      Neq(StrIdentifier(id.str), StrLiteral(lit.str))
    }
  }

  def or: QueryParser.Parser[OrExpr] = {
    (eq ~ OR ~ eq) ^^ { case lhs ~ or ~ rhs =>
      OrExpr(lhs, rhs)
    }
  }

  def apply(tokens: Seq[QueryToken]): Either[QueryParserError, QueryAST] = {
    val reader = new QueryTokenReader(tokens)
    program(reader) match {
      case NoSuccess(msg, next)  => Left(QueryParserError(msg))
      case Success(result, next) => Right(result)
    }
  }
