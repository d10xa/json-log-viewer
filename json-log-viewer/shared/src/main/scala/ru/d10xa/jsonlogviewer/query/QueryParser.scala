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

  private def equal: Parser[QueryToken] = {
    accept("equal", { case i @ EQUAL => i })
  }
  private def notEqual: Parser[QueryToken] = {
    accept("not equal", { case i @ NOT_EQUAL => i })
  }

  def block: Parser[QueryAST] = {
    rep1(eq | neq) ^^ { stmtList => stmtList.head } // TODO
  }

  def program: Parser[QueryAST] = {
    phrase(block)
  }

  def eq: QueryParser.Parser[Eq] = {
    (identifier ~ equal ~ literal) ^^ { case id ~ eq ~ lit =>
      Eq(StrIdentifier(id.str), StrLiteral(lit.str))
    }
  }
  def neq: QueryParser.Parser[Neq] = {
    (identifier ~ notEqual ~ literal) ^^ { case id ~ neq ~ lit =>
      Neq(StrIdentifier(id.str), StrLiteral(lit.str))
    }
  }

  def apply(tokens: Seq[QueryToken]): Either[QueryParserError, QueryAST] = {
    val reader = new QueryTokenReader(tokens)
    program(reader) match {
      case NoSuccess(msg, next)  => Left(QueryParserError(msg))
      case Success(result, next) => Right(result)
    }
  }
