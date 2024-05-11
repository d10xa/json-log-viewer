package ru.d10xa.jsonlogviewer.logfmt

import scala.util.parsing.combinator.PackratParsers

object LogFmtTokenParser extends PackratParsers:
  override type Elem = LogFmtToken

  private def textAst: Parser[TextAst] =
    accept("text", { case a @ Text(v) => TextAst(v) })

  private def equalSignAst: Parser[EqualSignAst] =
    accept("equalSign", { case a @ EqualSign() => EqualSignAst() })

  private def spacesAst: Parser[SpacesAst] =
    accept("spaces", { case a @ Spaces(s) => SpacesAst(s) })

  private def doubleQuoteAst: Parser[DoubleQuoteAst] =
    accept("doubleQuote", { case a @ DoubleQuote() => DoubleQuoteAst() })

  private def valueAst: Parser[LogFmtAst] =
    quotedValueAst | unquotedValueAst

  private def unquotedValueAst: Parser[UnquotedValueAst] =
    textAst ^^ { case TextAst(value) =>
      UnquotedValueAst(value)
    }

  private def quotedValueAst: Parser[LogFmtAst] =
    (doubleQuoteAst ~> rep(
      escapedDoubleQuoteAst | spacesAst | textAst
    ) <~ doubleQuoteAst) ^^ { case segments: Seq[LogFmtAst] =>
      QuotedValueAst(segments.toVector)
    }

  private def escapedDoubleQuoteAst: Parser[EscapedDoubleQuoteAst] =
    accept(
      "escapedDoubleQuote",
      { case a @ EscapedDoubleQuote() => EscapedDoubleQuoteAst() }
    )

  def pairAst: Parser[PairAst] =
    (textAst ~ equalSignAst ~ valueAst) ^^ { case key ~ eq ~ value =>
      PairAst(key.toString, value)
    }

  def simpleAst: LogFmtTokenParser.Parser[LogFmtAst] = accept(
    "literal",
    {
      case lit @ Text(name)                          => AstString(name)
      case space @ Spaces(s)                         => SpacesAst(s)
      case doubleQuote @ DoubleQuote()               => DoubleQuoteAst()
      case escapedDoubleQuote @ EscapedDoubleQuote() => EscapedDoubleQuoteAst()
      case equalSign @ EqualSign()                   => EqualSignAst()
    }
  )

  def statement: LogFmtTokenParser.Parser[LogFmtAst] =
    pairAst | simpleAst

  def program: Parser[LogFmtAst] = phrase(
    rep1(statement).^^(list => StatementsAst(list.toVector))
  )

  def apply(tokens: Seq[LogFmtToken]): Either[TokenParserError, LogFmtAst] =
    val reader = new PackratReader(new LogFmtTokenReader(tokens))
    program(reader) match {
      case NoSuccess(msg, next)  => Left(TokenParserError(msg))
      case Success(result, next) => Right(result)
    }
