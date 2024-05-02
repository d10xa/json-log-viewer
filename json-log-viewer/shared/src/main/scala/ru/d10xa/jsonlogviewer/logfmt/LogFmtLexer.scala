package ru.d10xa.jsonlogviewer.logfmt

import scala.util.parsing.combinator.RegexParsers

object LogFmtLexer extends RegexParsers {
  override def skipWhitespace: Boolean = false

  def text: Parser[Text] = """[^"\s=]+""".r ^^ { s =>
    Text(s)
  }

  def escapedDoubleQuote: Parser[EscapedDoubleQuote] = "\\\"" ^^ { _ =>
    EscapedDoubleQuote()
  }

  def doubleQuote: Parser[DoubleQuote] = "\"" ^^ { _ => DoubleQuote() }

  def equalsSign: Parser[EqualSign] = "=" ^^ { _ => EqualSign() }
  def spaces: Parser[Spaces] = """\s+""".r ^^ { s => Spaces(s) }
  def tokens: Parser[List[LogFmtToken]] = phrase(
    rep1(escapedDoubleQuote | doubleQuote | equalsSign | spaces | text)
  )

  def parse(input: String): LogFmtLexer.ParseResult[List[LogFmtToken]] =
    parseAll(tokens, input)

  def apply(code: String): Either[TokenLexerError, List[LogFmtToken]] =
    parse(tokens, code) match {
      case NoSuccess(msg, next) =>
        Left(TokenLexerError(msg))
      case Success(result, next) =>
        Right(result)
    }
}
