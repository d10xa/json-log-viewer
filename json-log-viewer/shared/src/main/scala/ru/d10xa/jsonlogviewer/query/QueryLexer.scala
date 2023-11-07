package ru.d10xa.jsonlogviewer.query

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

object QueryLexer extends RegexParsers:

  override def skipWhitespace: Boolean = true

  override protected val whiteSpace: Regex = "[ \t\r\f\n]+".r

  def identifier: Parser[IDENTIFIER] = {
    "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => IDENTIFIER(str) }
  }

  def literal: Parser[LITERAL] = {
    """'[^']*'""".r ^^ { str =>
      val content = str.substring(1, str.length - 1)
      LITERAL(content)
    }
  }
  def equal: Parser[QueryToken] = { "=" ^^ (_ => EQUAL) }
  def notEqual: Parser[QueryToken] = { "!=" ^^ (_ => NOT_EQUAL) }
  def or: Parser[QueryToken] = { "OR" ^^ (_ => OR) }
  def and: Parser[QueryToken] = { "AND" ^^ (_ => AND) }
  def like: Parser[QueryToken] = { "LIKE" ^^ (_ => LIKE) }
  def notlike: Parser[QueryToken] = { "NOT LIKE" ^^ (_ => NOTLIKE) }
  def lparen: Parser[QueryToken] = { "(" ^^ (_ => LPAREN) }
  def rparen: Parser[QueryToken] = { ")" ^^ (_ => RPAREN) }

  def tokens: Parser[List[QueryToken]] =
    phrase(
      rep1(lparen | rparen | notlike |like | or | and | equal | notEqual | literal | identifier)
    ) ^^ identity

  def apply(code: String): Either[QueryLexerError, List[QueryToken]] =
    parse(tokens, code) match {
      case NoSuccess(msg, next)  =>
        Left(QueryLexerError(msg))
      case Success(result, next) =>
        Right(result)
    }
